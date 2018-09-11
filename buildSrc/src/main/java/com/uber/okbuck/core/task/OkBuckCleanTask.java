package com.uber.okbuck.core.task;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.MoreCollectors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/** A task to cleanup stale BUCK files */
@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused", "ResultOfMethodCallIgnored", "NewApi"})
public class OkBuckCleanTask extends DefaultTask {

  @Input public Set<String> currentProjectPaths;

  @Inject
  public OkBuckCleanTask(Set<String> currentProjectPaths) {
    this.currentProjectPaths = currentProjectPaths;
  }

  @TaskAction
  void clean() throws IOException {
    Project rootProject = getProject();
    Path rootProjectPath = rootProject.getProjectDir().toPath();

    File okbuckState = rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE);

    // Get last project paths
    Set<String> lastProjectPaths;
    if (okbuckState.exists()) {
      try (Stream<String> lines = Files.lines(okbuckState.toPath())) {
        lastProjectPaths =
            lines
                .map(String::trim)
                .filter(s -> !Strings.isNullOrEmpty(s))
                .collect(MoreCollectors.toImmutableSet());
      }
    } else {
      lastProjectPaths = ImmutableSet.of();
      okbuckState.getParentFile().mkdirs();
      okbuckState.createNewFile();
    }

    Sets.SetView<String> difference = Sets.difference(lastProjectPaths, currentProjectPaths);

    // Delete stale project's BUCK file
    difference
        .stream()
        .map(p -> rootProjectPath.resolve(p).resolve(OkBuckGradlePlugin.BUCK))
        .forEach(FileUtil::deleteQuietly);

    // Delete old .okbuck/cache dir
    FileUtil.deleteQuietly(rootProjectPath.resolve(".okbuck/cache"));

    // Save generated project's BUCK file path
    Files.write(
        okbuckState.toPath(),
        currentProjectPaths.stream().sorted().collect(MoreCollectors.toImmutableList()));
  }

  @Override
  public String getDescription() {
    return "Delete stale configuration files generated by OkBuck";
  }

  @Override
  public String getGroup() {
    return OkBuckGradlePlugin.GROUP;
  }
}
