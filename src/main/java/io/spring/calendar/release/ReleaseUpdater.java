/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.calendar.release;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {@code ReleaseUpdater} updates the known {@link Release Releases}.
 *
 * @author Andy Wilkinson
 */
@Component
class ReleaseUpdater {

	private static final Logger log = LoggerFactory.getLogger(ReleaseUpdater.class);

	private final List<Supplier<List<ProjectReleases>>> projectReleasesSuppliers;

	private final ReleaseRepository releaseRepository;

	private final ProjectNameAliaser projectNameAliaser;

	ReleaseUpdater(List<Supplier<List<ProjectReleases>>> projectReleasesSuppliers, ReleaseRepository releaseRepository,
			ProjectNameAliaser projectNameAliaser) {
		this.projectReleasesSuppliers = projectReleasesSuppliers;
		this.releaseRepository = releaseRepository;
		this.projectNameAliaser = projectNameAliaser;
	}

	@Scheduled(fixedRate = 5 * 60 * 1000)
	void updateReleases() {
		log.info("Updating releases");
		Map<String, ProjectReleases> releasesByProject = new HashMap<>();
		this.projectReleasesSuppliers //
				.stream() //
				.map(Supplier::get) //
				.flatMap(List::stream) //
				.forEach((projectReleases) -> {
					ProjectReleases existing = releasesByProject.putIfAbsent(projectReleases.getProject(),
							projectReleases);
					if (existing != null) {
						existing.getReleases().addAll(projectReleases.getReleases());
					}
				});
		List<Release> releases = releasesByProject.values().stream()
				.flatMap((projectReleases) -> projectReleases.getReleases().stream()) //
				.map(this::applyNameAlias) //
				.collect(Collectors.toList());
		updateReleases(releases);
		log.info("Releases updated");
	}

	private Release applyNameAlias(Release release) {
		return new Release(this.projectNameAliaser.apply(release.getProject()), release.getName(), release.getDate(),
				release.getStatus(), release.getUrl());

	}

	private void updateReleases(List<Release> releases) {
		this.releaseRepository.set(releases);
	}

}
