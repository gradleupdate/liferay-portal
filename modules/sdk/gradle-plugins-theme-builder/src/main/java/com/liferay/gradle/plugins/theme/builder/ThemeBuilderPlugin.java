/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.gradle.plugins.theme.builder;

import com.liferay.gradle.plugins.css.builder.BuildCSSTask;
import com.liferay.gradle.plugins.css.builder.CSSBuilderPlugin;
import com.liferay.gradle.plugins.theme.builder.tasks.BuildThemeTask;
import com.liferay.gradle.plugins.theme.builder.tasks.BuildThumbnailsTask;
import com.liferay.gradle.util.GradleUtil;

import java.io.File;

import java.util.Collections;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.War;

/**
 * @author Andrea Di Giorgi
 */
public class ThemeBuilderPlugin implements Plugin<Project> {

	public static final String BUILD_THEME_TASK_THEME = "buildTheme";

	public static final String BUILD_THUMBNAILS_TASK_NAME = "buildThumbnails";

	public static final String FRONTEND_THEMES_CONFIGURATION = "frontendThemes";

	public static final String THUMBNAILS_BUILDER_CONFIGURATION_NAME =
		"thumbnailsBuilder";

	@Override
	public void apply(Project project) {
		GradleUtil.applyPlugin(project, CSSBuilderPlugin.class);
		GradleUtil.applyPlugin(project, WarPlugin.class);

		Configuration frontendThemesConfiguration =
			addConfigurationFrontendThemes(project);
		Configuration thumbnailsBuilderConfiguation =
			addConfigurationThumbnailsBuilder(project);

		BuildThemeTask buildThemeTask = addTaskBuildTheme(
			project, frontendThemesConfiguration);

		BuildThumbnailsTask buildThumbnailsTask = addTaskBuildThumbnails(
			project, buildThemeTask);

		configureTaskBuildCSS(project, buildThemeTask);
		configureTaskWar(project, buildThemeTask, buildThumbnailsTask);
		configureTasksBuildThumbnails(project, thumbnailsBuilderConfiguation);
	}

	protected Configuration addConfigurationFrontendThemes(
		final Project project) {

		Configuration configuration = GradleUtil.addConfiguration(
			project, FRONTEND_THEMES_CONFIGURATION);

		configuration.defaultDependencies(
			new Action<DependencySet>() {

				@Override
				public void execute(DependencySet dependencySet) {
					addDependenciesFrontendThemes(project);
				}

			});

		configuration.setDescription("Configures the parent themes.");
		configuration.setVisible(false);

		return configuration;
	}

	protected Configuration addConfigurationThumbnailsBuilder(
		final Project project) {

		Configuration configuration = GradleUtil.addConfiguration(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME);

		configuration.defaultDependencies(
			new Action<DependencySet>() {

				@Override
				public void execute(DependencySet dependencySet) {
					addDependenciesThumbnailsBuilder(project);
				}

			});

		configuration.setDescription(
			"Configures Liferay Thumbnails Builder for this project.");
		configuration.setVisible(false);

		return configuration;
	}

	protected void addDependenciesFrontendThemes(Project project) {
		GradleUtil.addDependency(
			project, FRONTEND_THEMES_CONFIGURATION, "com.liferay",
			"com.liferay.frontend.theme.styled", "latest.release");
		GradleUtil.addDependency(
			project, FRONTEND_THEMES_CONFIGURATION, "com.liferay",
			"com.liferay.frontend.theme.unstyled", "latest.release");
	}

	protected void addDependenciesThumbnailsBuilder(Project project) {
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME, "com.liferay",
			"org.monte", "0.7.7");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME,
			"com.liferay.portal", "com.liferay.portal.impl", "latest.release");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME,
			"com.liferay.portal", "com.liferay.portal.kernel",
			"latest.release");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME,
			"com.liferay.portal", "com.liferay.util.java", "latest.release");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME,
			"com.thoughtworks.xstream", "xstream", "1.4.3");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME,
			"commons-configuration", "commons-configuration", "1.6");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME, "commons-io",
			"commons-io", "2.1");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME, "commons-lang",
			"commons-lang", "2.6");
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME, "easyconf",
			"easyconf", "0.9.5", false);
		GradleUtil.addDependency(
			project, THUMBNAILS_BUILDER_CONFIGURATION_NAME, "javax.servlet",
			"javax.servlet-api", "3.0.1");
	}

	protected BuildThemeTask addTaskBuildTheme(
		final Project project, FileCollection frontendThemeFiles) {

		BuildThemeTask buildThemeTask = GradleUtil.addTask(
			project, BUILD_THEME_TASK_THEME, BuildThemeTask.class);

		buildThemeTask.setDescription(
			"Merges the parent theme with the project source files.");

		buildThemeTask.setDiffsDir(
			new Callable<File>() {

				@Override
				public File call() throws Exception {
					WarPluginConvention warPluginConvention =
						GradleUtil.getConvention(
							project, WarPluginConvention.class);

					return warPluginConvention.getWebAppDir();
				}

			});

		buildThemeTask.setFrontendThemeFiles(frontendThemeFiles);
		buildThemeTask.setThemeParent(BuildThemeTask.PORTAL_THEME_STYLED);
		buildThemeTask.setThemeRootDir(
			new File(project.getBuildDir(), "built-theme"));
		buildThemeTask.setThemeTypes(Collections.singleton("ftl"));

		return buildThemeTask;
	}

	protected BuildThumbnailsTask addTaskBuildThumbnails(
		Project project, final BuildThemeTask buildThemeTask) {

		BuildThumbnailsTask buildThumbnailsTask = GradleUtil.addTask(
			project, BUILD_THUMBNAILS_TASK_NAME, BuildThumbnailsTask.class);

		buildThumbnailsTask.dependsOn(buildThemeTask);

		buildThumbnailsTask.setDescription("Builds the thumbnail files.");
		buildThumbnailsTask.setIncludes(
			Collections.singleton("**/screenshot.png"));

		buildThumbnailsTask.setSource(
			new Callable<File>() {

				@Override
				public File call() throws Exception {
					return buildThemeTask.getThemeRootDir();
				}

			});

		return buildThumbnailsTask;
	}

	protected void configureTaskBuildCSS(
		Project project, final BuildThemeTask buildThemeTask) {

		BuildCSSTask buildCSSTask = (BuildCSSTask)GradleUtil.getTask(
			project, CSSBuilderPlugin.BUILD_CSS_TASK_NAME);

		buildCSSTask.dependsOn(buildThemeTask);

		// This dependency is not automatically added if docrootDir is empty
		// at configuration phase

		buildCSSTask.dependsOn(
			CSSBuilderPlugin.EXPAND_PORTAL_COMMON_CSS_TASK_NAME);

		buildCSSTask.setDocrootDir(
			new Callable<File>() {

				@Override
				public File call() throws Exception {
					return buildThemeTask.getThemeRootDir();
				}

			});
	}

	protected void configureTaskBuildThumbnailsClasspath(
		BuildThumbnailsTask buildThumbnailsTask, FileCollection classpath) {

		buildThumbnailsTask.setClasspath(classpath);
	}

	protected void configureTasksBuildThumbnails(
		Project project, final FileCollection classpath) {

		TaskContainer taskContainer = project.getTasks();

		taskContainer.withType(
			BuildThumbnailsTask.class,
			new Action<BuildThumbnailsTask>() {

				@Override
				public void execute(BuildThumbnailsTask buildThumbnailsTask) {
					configureTaskBuildThumbnailsClasspath(
						buildThumbnailsTask, classpath);
				}

			});
	}

	protected void configureTaskWar(
		Project project, final BuildThemeTask buildThemeTask,
		BuildThumbnailsTask buildThumbnailsTask) {

		War war = (War)GradleUtil.getTask(project, WarPlugin.WAR_TASK_NAME);

		war.dependsOn(buildThemeTask, buildThumbnailsTask);
		war.exclude("**/*.scss");

		war.filesMatching(
			"**/.sass-cache/",
			new Action<FileCopyDetails>() {

				@Override
				public void execute(FileCopyDetails fileCopyDetails) {
					String path = fileCopyDetails.getPath();

					path = path.replace(".sass-cache/", "");

					fileCopyDetails.setPath(path);
				}

			});

		war.from(
			new Callable<File>() {

				@Override
				public File call() throws Exception {
					return buildThemeTask.getThemeRootDir();
				}

			});

		war.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
		war.setIncludeEmptyDirs(false);
	}

}