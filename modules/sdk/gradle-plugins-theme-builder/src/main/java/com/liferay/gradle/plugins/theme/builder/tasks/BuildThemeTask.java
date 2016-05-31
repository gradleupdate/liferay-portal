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

package com.liferay.gradle.plugins.theme.builder.tasks;

import com.liferay.gradle.util.ArrayUtil;
import com.liferay.gradle.util.GradleUtil;
import com.liferay.gradle.util.StringUtil;
import com.liferay.gradle.util.Validator;
import com.liferay.gradle.util.copy.StripPathSegmentsAction;

import groovy.lang.Closure;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GUtil;

/**
 * @author Andrea Di Giorgi
 */
public class BuildThemeTask extends DefaultTask {

	public static final String PORTAL_THEME_ADMIN = "admin";

	public static final String PORTAL_THEME_CLASSIC = "classic";

	public static final String PORTAL_THEME_STYLED = "_styled";

	public static final String PORTAL_THEME_UNSTYLED = "_unstyled";

	@TaskAction
	public void compileTheme() throws Exception {
		copyThemeParent();

		copyDiffs();
	}

	@InputDirectory
	@Optional
	public File getDiffsDir() {
		return GradleUtil.toFile(getProject(), _diffsDir);
	}

	@InputFiles
	@Optional
	public FileCollection getFrontendThemeFiles() {
		return _frontendThemeFiles;
	}

	@InputDirectory
	@Optional
	public File getFrontendThemesWebDir() {
		return GradleUtil.toFile(getProject(), _frontendThemesWebDir);
	}

	@OutputDirectories
	public FileCollection getThemeDirs() {
		Project project = getProject();

		if (getDiffsDir() == null) {
			return project.files();
		}

		List<File> themeDirs = new ArrayList<>(_THEME_DIR_NAMES.length);

		File themeRootDir = getThemeRootDir();

		for (String dirName : _THEME_DIR_NAMES) {
			File dir = new File(themeRootDir, dirName);

			themeDirs.add(dir);
		}

		return project.files(themeDirs);
	}

	@Input
	@Optional
	public String getThemeParent() {
		return GradleUtil.toString(_themeParent);
	}

	public Project getThemeParentProject() {
		String themeParent = getThemeParent();

		if (Validator.isNull(themeParent) ||
			ArrayUtil.contains(_PORTAL_THEMES, themeParent)) {

			return null;
		}

		if (_themeParentProject == null) {
			Project project = getProject();

			File themeParentDir = project.file(themeParent);

			_themeParentProject = GradleUtil.getProject(
				project.getRootProject(), themeParentDir);
		}

		return _themeParentProject;
	}

	public File getThemeRootDir() {
		return GradleUtil.toFile(getProject(), _themeRootDir);
	}

	@Input
	public Set<String> getThemeTypes() {
		return _themeTypes;
	}

	public void setDiffsDir(Object diffsDir) {
		_diffsDir = diffsDir;
	}

	public void setFrontendThemeFiles(FileCollection frontendThemeFiles) {
		_frontendThemeFiles = frontendThemeFiles;
	}

	public void setFrontendThemesWebDir(Object frontendThemesWebDir) {
		_frontendThemesWebDir = frontendThemesWebDir;
	}

	public void setThemeParent(Object themeParent) {
		_themeParent = themeParent;
		_themeParentProject = null;
	}

	public void setThemeRootDir(Object themeRootDir) {
		_themeRootDir = themeRootDir;
	}

	public void setThemeTypes(Iterable<String> themeTypes) {
		_themeTypes.clear();

		themeTypes(themeTypes);
	}

	public BuildThemeTask themeTypes(Iterable<String> themeTypes) {
		GUtil.addToCollection(_themeTypes, themeTypes);

		return this;
	}

	public BuildThemeTask themeTypes(String... themeTypes) {
		return themeTypes(Arrays.asList(themeTypes));
	}

	protected void copyDiffs() {
		final File diffsDir = getDiffsDir();

		if ((diffsDir == null) || !diffsDir.exists()) {
			return;
		}

		Project project = getProject();

		Closure<Void> closure = new Closure<Void>(null) {

			@SuppressWarnings("unused")
			public void doCall(CopySpec copySpec) {
				for (String dirName : _THEME_DIR_NAMES) {
					copySpec.include(dirName + "/");
				}

				copySpec.from(diffsDir);
				copySpec.into(getThemeRootDir());
			}

		};

		project.copy(closure);
	}

	protected void copyPortalThemeDir(
			String theme, String[] excludes, String include)
		throws Exception {

		copyPortalThemeDir(theme, excludes, new String[] {include});
	}

	protected void copyPortalThemeDir(
			String theme, final String[] excludes, final String[] includes)
		throws Exception {

		final Project project = getProject();

		final String prefix = theme + "/";

		final File frontendThemesWebDir = getFrontendThemesWebDir();

		if (frontendThemesWebDir != null) {
			Closure<Void> closure = new Closure<Void>(null) {

				@SuppressWarnings("unused")
				public void doCall(CopySpec copySpec) {
					copySpec.from(new File(frontendThemesWebDir, prefix));

					if (ArrayUtil.isNotEmpty(excludes)) {
						copySpec.exclude(excludes);
					}

					copySpec.include(includes);
					copySpec.into(getThemeRootDir());
				}

			};

			project.copy(closure);
		}
		else {
			String jarPrefix = "META-INF/resources/" + prefix;

			final File frontendThemeFile = getFrontendThemeFile(theme);
			final String[] prefixedExcludes = StringUtil.prepend(
				excludes, jarPrefix);
			final String[] prefixedIncludes = StringUtil.prepend(
				includes, jarPrefix);

			Closure<Void> closure = new Closure<Void>(null) {

				@SuppressWarnings("unused")
				public void doCall(CopySpec copySpec) {
					copySpec.eachFile(new StripPathSegmentsAction(3));

					if (ArrayUtil.isNotEmpty(prefixedExcludes)) {
						copySpec.exclude(prefixedExcludes);
					}

					copySpec.from(project.zipTree(frontendThemeFile));
					copySpec.include(prefixedIncludes);
					copySpec.into(getThemeRootDir());
					copySpec.setIncludeEmptyDirs(false);
				}

			};

			project.copy(closure);
		}
	}

	protected void copyThemeParent() throws Exception {
		String themeParent = getThemeParent();

		if (Validator.isNull(themeParent)) {
			return;
		}

		if (themeParent.equals(PORTAL_THEME_STYLED) ||
			themeParent.equals(PORTAL_THEME_UNSTYLED)) {

			copyThemeParentUnstyled();
		}

		if (themeParent.equals(PORTAL_THEME_STYLED)) {
			copyThemeParentStyled();
		}
		else if (themeParent.equals(PORTAL_THEME_ADMIN) ||
				 themeParent.equals(PORTAL_THEME_CLASSIC)) {

			copyThemeParentPortal();
		}
	}

	protected void copyThemeParentPortal() throws Exception {
		String themeParent = getThemeParent();

		copyPortalThemeDir(
			themeParent,
			new String[] {"**/.sass-cache/**", "_diffs/**", "templates/**"},
			"**");

		Set<String> themeTypes = getThemeTypes();

		String[] includes = StringUtil.prepend(
			themeTypes.toArray(new String[themeTypes.size()]), "templates/*.");

		copyPortalThemeDir(themeParent, null, includes);
	}

	protected void copyThemeParentStyled() throws Exception {
		copyPortalThemeDir(
			PORTAL_THEME_STYLED,
			new String[] {"**/*.css", "npm-debug.log", "package.json"}, "**");
	}

	protected void copyThemeParentUnstyled() throws Exception {
		copyPortalThemeDir(
			PORTAL_THEME_UNSTYLED,
			new String[] {
				"**/*.css", "npm-debug.log", "package.json", "templates/**"
			},
			"**");

		Set<String> themeTypes = getThemeTypes();

		String[] themeTypesArray = themeTypes.toArray(
			new String[themeTypes.size()]);

		String[] excludes = null;

		if (getFrontendThemesWebDir() != null) {
			excludes = StringUtil.prepend(themeTypesArray, "templates/init.");
		}

		String[] includes = StringUtil.prepend(
			themeTypesArray, "templates/**/*.");

		copyPortalThemeDir(PORTAL_THEME_UNSTYLED, excludes, includes);
	}

	protected File getFrontendThemeFile(String theme) throws Exception {
		for (File file : getFrontendThemeFiles()) {
			try (JarFile jarFile = new JarFile(file)) {
				JarEntry jarEntry = jarFile.getJarEntry(
					"META-INF/resources/" + theme);

				if (jarEntry != null) {
					return file;
				}
			}
		}

		return null;
	}

	private static final String[] _PORTAL_THEMES = {
		PORTAL_THEME_ADMIN, PORTAL_THEME_CLASSIC, PORTAL_THEME_STYLED,
		PORTAL_THEME_UNSTYLED
	};

	private static final String[] _THEME_DIR_NAMES = {
		"css", "images", "js", "templates"
	};

	private Object _diffsDir;
	private FileCollection _frontendThemeFiles;
	private Object _frontendThemesWebDir;
	private Object _themeParent;
	private Project _themeParentProject;
	private Object _themeRootDir;
	private final Set<String> _themeTypes = new HashSet<>();

}