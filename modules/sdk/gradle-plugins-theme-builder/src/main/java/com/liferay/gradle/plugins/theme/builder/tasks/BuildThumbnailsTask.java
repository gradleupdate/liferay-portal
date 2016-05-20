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

import com.liferay.gradle.util.FileUtil;
import com.liferay.gradle.util.GradleUtil;

import groovy.lang.Closure;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaExecSpec;

/**
 * @author Andrea Di Giorgi
 */
public class BuildThumbnailsTask extends SourceTask {

	@TaskAction
	public void buildThumbnails() {
		for (File file : getSource()) {
			buildThumbnail(file);
		}
	}

	@InputFiles
	public FileCollection getClasspath() {
		return _classpath;
	}

	@Input
	public int getHeight() {
		return _height;
	}

	public Closure<?> getJavaExecClosure() {
		return _javaExecClosure;
	}

	public String getThumbnailFileName() {
		return GradleUtil.toString(_thumbnailFileName);
	}

	@OutputFiles
	public Iterable<File> getThumbnailFiles() {
		List<File> thumbnailFiles = new ArrayList<>();

		for (File file : getSource()) {
			thumbnailFiles.add(getThumbnailFile(file));
		}

		return thumbnailFiles;
	}

	@Input
	public int getWidth() {
		return _width;
	}

	@Input
	public boolean isOverwrite() {
		return _overwrite;
	}

	public void setClasspath(FileCollection classpath) {
		_classpath = classpath;
	}

	public void setHeight(int height) {
		_height = height;
	}

	public void setJavaExecClosure(Closure<?> javaExecClosure) {
		_javaExecClosure = javaExecClosure;
	}

	public void setOverwrite(boolean overwrite) {
		_overwrite = overwrite;
	}

	public void setThumbnailFileName(Object thumbnailFileName) {
		_thumbnailFileName = thumbnailFileName;
	}

	public void setWidth(int width) {
		_width = width;
	}

	protected void buildThumbnail(final File file) {
		Project project = getProject();

		project.javaexec(
			new Action<JavaExecSpec>() {

				@Override
				public void execute(JavaExecSpec javaExecSpec) {
					javaExecSpec.args("thumbnail.height=" + getHeight());
					javaExecSpec.args(
						"thumbnail.original.file=" +
							FileUtil.relativize(
								file, javaExecSpec.getWorkingDir()));
					javaExecSpec.args("thumbnail.overwrite=" + isOverwrite());
					javaExecSpec.args(
						"thumbnail.thumbnail.file=" +
							FileUtil.relativize(
								getThumbnailFile(file),
								javaExecSpec.getWorkingDir()));
					javaExecSpec.args("thumbnail.width=" + getWidth());

					javaExecSpec.setClasspath(getClasspath());
					javaExecSpec.setMain(
						"com.liferay.portal.tools.ThumbnailBuilder");

					Closure<?> closure = getJavaExecClosure();

					if (closure != null) {
						closure.call(javaExecSpec);
					}
				}

			});
	}

	protected File getThumbnailFile(File file) {
		return new File(file.getParentFile(), getThumbnailFileName());
	}

	private FileCollection _classpath;
	private int _height = 120;
	private Closure<?> _javaExecClosure;
	private boolean _overwrite;
	private Object _thumbnailFileName = "thumbnail.png";
	private int _width = 160;

}