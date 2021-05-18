package io.sapl.mavenplugin.test.coverage.report.html;

import static j2html.TagCreator.a;
import static j2html.TagCreator.attrs;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.img;
import static j2html.TagCreator.li;
import static j2html.TagCreator.link;
import static j2html.TagCreator.main;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.textarea;
import static j2html.TagCreator.title;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;

import io.sapl.mavenplugin.test.coverage.PathHelper;
import io.sapl.mavenplugin.test.coverage.report.model.SaplDocumentCoverageInformation;
import j2html.attributes.Attribute;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class HtmlLineCoverageReportGenerator {

	private Collection<SaplDocumentCoverageInformation> documents;
	private Log log;
	private Path basedir;
	private float policySetHitRatio;
	private float policyHitRatio;
	private float policyConditionHitRatio;

	public Path generateHtmlReport() {
		Path index = generateMainSite();
		generateCustomCSS();
		copyAssets();
		for (var doc : this.documents) {
			generatePolicySite(doc);
		}
		return index;

	}

	private Path generateMainSite() {

		// @formatter:off
		ContainerTag mainSite = 
			html(
				head(
					meta().withCharset("utf-8"),
					meta().withName("viewport").withContent("width=device-width, initial-scale=1, shrink-to-fit=no"),
					link().withRel("stylesheet")
						.withHref("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css")
						.attr(new Attribute("integrity","sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"))
						.attr(new Attribute("crossorigin", "anonymous")),
					link().withRel("stylesheet").withHref("assets/main.css"),
					link().withRel("icon").withHref("assets/favicon.png").withType("image/png"),
					title("SAPL Coverage Report")),
				body(
					main(
						attrs("#main.content"),
						nav(
							a(
								img()
									.withSrc("assets/logo-header.png")
									.withStyle("display: inline-block; height: 60px; margin-right: 10px")
							)
								.withClass("navbar-brand")
								.withHref("#")
						)
							.withClass("navbar navbar-light")
							.withStyle("background-color: #20232a"),
						h1("SAPL Coverage Report").withStyle("padding: 1.25rem;"),

						div(
							div("SAPL Coverage Ratio").withClass("card-header"), 
							div(
								p("PolicySet Hit Ratio: " + policySetHitRatio + "%"),
								p("Policy Hit Ratio: " + policyHitRatio + "%"),
								p("PolicyCondition Hit Ratio: " + policyConditionHitRatio + "%")
							).withClass("card-body")
						).withClass("card").withStyle("padding: 1.25rem"),

						div(
							div("Show coverage per SAPL document").withClass("card-header"), 
							div(
								div(
									each(
										this.documents,
										document -> 
											a(document.getPathToDocument().getFileName().toString())
												.withHref("policies/" + document.getPathToDocument().getFileName().toString() + ".html")
												.withClass("list-group-item list-group-item-action")
									)
								).withClass("list-group")
							).withClass("card-body")
						).withClass("card").withStyle("padding: 1.25rem")
					),
					getJquery(),
					getPopper(),
					getBootstrapJs()
				)
			);
		// @formatter:on

		Path filePath = this.basedir.resolve("html").resolve("index.html");
		createFile(filePath, mainSite.render());
		return filePath;
	}

	private void generateCustomCSS() {
		String css = ".coverage-green span { background-color: #ccffcc; }\n"
				+ ".coverage-yellow span { background-color: #ffffcc; }\n" + ".coverage-red span { background-color: #ffaaaa; }\n"
				+ ".CodeMirror { height: calc(100% - 50px) !important; }\n";
		Path cssPath = this.basedir.resolve("html").resolve("assets").resolve("main.css");
		createFile(cssPath, css);
	}


	private void generatePolicySite(SaplDocumentCoverageInformation document) {

		List<String> lines = readPolicyDocument(document.getPathToDocument());

		List<HtmlPolicyLineModel> models = createHtmlPolicyLineModel(lines, document);

		String filename = document.getPathToDocument().getFileName().toString();
		ContainerTag policySite = createPolicySite_CodeMirror(filename, models);
		

		Path filePath = this.basedir.resolve("html").resolve("policies").resolve(filename + ".html");
		createFile(filePath, policySite.renderFormatted());
	}

	

	private ContainerTag createPolicySite_CodeMirror(String filename, List<HtmlPolicyLineModel> models) {

		StringBuilder wholeTextOfPolicy = new StringBuilder();
		StringBuilder htmlReportCodeMirrorJSLineClassStatements = new StringBuilder("\n");
		for (int i = 0; i < models.size(); i++) {
			var model = models.get(i);
			wholeTextOfPolicy.append(model.getLineContent() + "\n");
			htmlReportCodeMirrorJSLineClassStatements
					.append(String.format("editor.addLineClass(%s, \"text\", \"%s\");\n", i, model.getCssClass()));
			if(model.getPopoverContent() != null) {
				htmlReportCodeMirrorJSLineClassStatements
				    .append(String.format("editor.getDoc().markText({line:%s,ch:0},{line:%s,ch:%d},{attributes: { \"data-toggle\": \"popover\", \"data-trigger\": \"hover\", \"data-placement\": \"top\", \"data-content\": \"%s\" }})\n", i, i, model.getLineContent().toCharArray().length, model.getPopoverContent()));				
			}
		}

		var htmlReportCodeMirrorJsTemplatePath = getClass().getClassLoader()
				.getResourceAsStream("scripts/html-report-codemirror-template.js");
		String htmlReportCodeMirrorJsTemplate = new BufferedReader(
				new InputStreamReader(htmlReportCodeMirrorJsTemplatePath)).lines().collect(Collectors.joining("\n"));
		String htmlReportCoreMirrorJS = htmlReportCodeMirrorJsTemplate.replace("{{replacement}}", Stream.of(htmlReportCodeMirrorJSLineClassStatements).collect(Collectors.joining()));

		// @formatter:off
		return html(
			    head(
			        title("SAPL Coverage Report"),
			    	meta().withCharset("utf-8"),
			    	meta().withName("viewport").withContent("width=device-width, initial-scale=1, shrink-to-fit=no"),
					getBootstrapCss(),
			        link().withRel("stylesheet").withHref("../assets/main.css"),
			        link().withRel("stylesheet").withHref("../assets/codemirror.css"),
			        script().withSrc("../assets/require.js")
			        ),
			    body(
			        main(attrs("#main.content"),
	        			nav( 
        					ol(
    							li(
									a("Home").withHref("../index.html")
								).withClass("breadcrumb-item"), 
    							li(
									filename
								).withClass("breadcrumb-item active").attr(new Attribute("aria-current", "page"))
							).withClass("breadcrumb")
    					).attr(new Attribute("aria-label", "breadcrumb")),
			            div(
		            		h1(filename).withStyle("margin-bottom: 2vw"),
		            		textarea(wholeTextOfPolicy.toString()).withId("policyTextArea")
	            		).withClass("card-body").withStyle("height: 80%")
			        ).withStyle("height: 100vh"),
					//getJquery(),
					//getPopper(),
					//getBootstrapJs(),
	        		script().with(rawHtml(htmlReportCoreMirrorJS))
			    )
			);
		// @formatter:on

	}

	private List<HtmlPolicyLineModel> createHtmlPolicyLineModel(List<String> lines,
			SaplDocumentCoverageInformation document) {
		List<HtmlPolicyLineModel> models = new LinkedList<HtmlPolicyLineModel>();
				
		for (int i = 0; i < lines.size(); i++) {
			var model = new HtmlPolicyLineModel();
			model.setLineContent(lines.get(i));
			var line = document.getLine(i + 1);
			switch (line.getCoveredValue()) {
			case FULLY:
				model.setCssClass("coverage-green");
				break;
			case NEVER:
				model.setCssClass("coverage-red");
				break;
			case PARTLY:
				model.setCssClass("coverage-yellow");
				model.setPopoverContent(String.format("%d of %d branches covered", line.getCoveredBranches(), line.getBranchesToCover()));
				break;
			case UNINTERESTING:
				model.setCssClass("");
				break;
			}
			models.add(model);
		}
		return models;
	}
	

	private void copyAssets() {
		Path logoHeaderpath = this.basedir.resolve("html").resolve("assets").resolve("logo-header.png");
		var logoSourcePath = getClass().getClassLoader().getResourceAsStream("images/logo-header.png");
		copyFile(logoSourcePath, logoHeaderpath);

		Path faviconPath = this.basedir.resolve("html").resolve("assets").resolve("favicon.png");
		var faviconSourcePath = getClass().getClassLoader().getResourceAsStream("images/favicon.png");
		copyFile(faviconSourcePath, faviconPath);

		Path requireJSTargetPath = this.basedir.resolve("html").resolve("assets").resolve("require.js");
		var requireJS = getClass().getClassLoader().getResourceAsStream("scripts/require.js");
		copyFile(requireJS, requireJSTargetPath);

		Path saplCodeMirrorModeJSTargetPath = this.basedir.resolve("html").resolve("assets").resolve("sapl-mode.js");
		var saplCodeMirrorModeJS = getClass().getClassLoader().getResourceAsStream("dependency-resources/sapl-mode.js");
		copyFile(saplCodeMirrorModeJS, saplCodeMirrorModeJSTargetPath);
				
		Path saplCodeMirrorSimpleAddonTargetPath = this.basedir.resolve("html").resolve("assets").resolve("codemirror").resolve("addon").resolve("mode").resolve("simple.js");
		var saplCodeMirrorSimpleAddon = getClass().getClassLoader().getResourceAsStream("scripts/simple.js");
		copyFile(saplCodeMirrorSimpleAddon, saplCodeMirrorSimpleAddonTargetPath);
		
		Path saplCodeMirrorJsTargetPath = this.basedir.resolve("html").resolve("assets").resolve("codemirror").resolve("lib").resolve("codemirror.js");
		var saplCodeMirrorJs = getClass().getClassLoader().getResourceAsStream("scripts/codemirror.js");
		copyFile(saplCodeMirrorJs, saplCodeMirrorJsTargetPath);
		
		Path saplCodeMirrorCssTargetPath = this.basedir.resolve("html").resolve("assets").resolve("codemirror.css");
		var saplCodeMirrorCss = getClass().getClassLoader().getResourceAsStream("scripts/codemirror.css");
		copyFile(saplCodeMirrorCss, saplCodeMirrorCssTargetPath);
		

		Path jqueryTargetPath = this.basedir.resolve("html").resolve("assets").resolve("jquery-3.2.1.slim.min.js");
		var jquery = getClass().getClassLoader().getResourceAsStream("scripts/jquery-3.2.1.slim.min.js");
		copyFile(jquery, jqueryTargetPath);
		
		Path popperTargetPath = this.basedir.resolve("html").resolve("assets").resolve("popper.min.js");
		var popper = getClass().getClassLoader().getResourceAsStream("scripts/popper.min.js");
		copyFile(popper, popperTargetPath);
		
		Path bootstrapTargetPath = this.basedir.resolve("html").resolve("assets").resolve("bootstrap.min.js");
		var bootstrap = getClass().getClassLoader().getResourceAsStream("scripts/bootstrap.min.js");
		copyFile(bootstrap, bootstrapTargetPath);
		
		Path bootstrapCssTargetPath = this.basedir.resolve("html").resolve("assets").resolve("bootstrap.min.css");
		var bootstrapCss = getClass().getClassLoader().getResourceAsStream("scripts/bootstrap.min.css");
		copyFile(bootstrapCss, bootstrapCssTargetPath);
	}

	private List<String> readPolicyDocument(Path filePath) {
		try {
			return Files.readAllLines(filePath);
		} catch (IOException e) {
			this.log.error(String.format("Error reading file: \"%s\"", filePath.toString()), e);
		}
		return new LinkedList<>();
	}

	private void createFile(Path filePath, String content) {
		if (!filePath.toFile().exists()) {
			PathHelper.createFile(filePath, log);
		}
		try {
			Files.writeString(filePath, content);
		} catch (IOException e) {
			this.log.error(String.format("Error writing file \"%s\"", filePath.toString()));
		}
	}

	private void copyFile(InputStream source, Path target) {
		try {
			if (target.getParent() != null && target.getParent().toFile() != null
					&& !target.getParent().toFile().exists()) {
				PathHelper.creatParentDirs(target, log);
			}
			if (!target.toFile().exists()) {
				Files.copy(source, target);
			}
		} catch (IOException e) {
			this.log.error(String.format("Error writing file \"%s\"", target.toString()));
		}
	}


	private ContainerTag getJquery() {
		return script().withSrc("../assets/jquery-3.2.1.slim.min.js");
	}
	
	private ContainerTag getPopper() {
		return script().withSrc("../assets/popper.min.js");
	}
	
	private ContainerTag getBootstrapJs() {
		return script().withSrc("../assets/bootstrap.min.js");
	}
	
	private EmptyTag getBootstrapCss() {
		return link().withRel("stylesheet").withHref("../assets/bootstrap.min.css");
	}
	
	@Data
	static class HtmlPolicyLineModel {
		String lineContent;
		String cssClass;
		String popoverContent;
	}
	
	/*
	private ContainerTag createPolicySite_GoogleCodePrettify(String filename, List<HtmlPolicyLineModel> models) {
		// @formatter:off
		return html(
				head(
					title("SAPL Coverage Report"), meta().withCharset("utf-8"),
					meta().withName("viewport").withContent("width=device-width, initial-scale=1, shrink-to-fit=no"),
					getBootstrapCss(),
					link().withRel("stylesheet").withHref("../assets/main.css"),
					script().withSrc("https://cdn.jsdelivr.net/gh/google/code-prettify@master/loader/run_prettify.js")
				),
				body(
					main(
						attrs("#main.content"),
						nav(
							ol(
								li(
									a("Home").withHref("../index.html")
								).withClass("breadcrumb-item"),
								li(filename).withClass("breadcrumb-item active").attr(new Attribute("aria-current", "page"))
							).withClass("breadcrumb")).attr(new Attribute("aria-label", "breadcrumb")),
						div(
							h1(filename).withStyle("margin-bottom: 2vw"),
							pre(
								each(models,
										model -> span(model.getLineContent() + "\n").withClass(model.getCssClass())
								)
							).withClass("source prettyprint linenums")
						).withClass("card-body")
					),
					script().withSrc("../assets/sapl-mode.js"),
					getJquery(),
					getPopper(),
					getBootstrapJs()
				)
			);

		// @formatter:on

	}
	*/
}