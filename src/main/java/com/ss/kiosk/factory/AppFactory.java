/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ss.kiosk.factory;

import com.ss.kiosk.FxInitializer;
import com.ss.kiosk.config.RenderConfig;
import com.ss.kiosk.config.LocalCacheConfig;
import com.ss.kiosk.service.ImageRotationService;
import com.ss.kiosk.service.LocalCacheService;
import com.ss.kiosk.view.ImageViewer;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Factory
public class AppFactory {

    @Singleton
    public @NotNull ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Singleton
    public @NotNull ImageRotationService imageRotationService(@NotNull LocalCacheService localCacheService) {
        return new ImageRotationService(localCacheService);
    }

    @Inject
    @Singleton
    public @NotNull LocalCacheService localCacheService(
        @NotNull LocalCacheConfig config,
        @NotNull ScheduledExecutorService scheduledExecutorService
    ) {

        var cacheService = new LocalCacheService(Paths.get(config.getFolder()));
        cacheService.initializeAndLoad();

        log.info(
            """
            Start cache service with settings: {
                reload-interval: {}
            }""",
            config.getReloadInterval()
        );

        scheduledExecutorService.scheduleWithFixedDelay(
            cacheService::reload,
            config.getReloadInterval(),
            config.getReloadInterval(),
            TimeUnit.SECONDS
        );

        return cacheService;
    }

    @Singleton
    public @NotNull Stage rootWindow() {
        return FxInitializer.getStage();
    }

    @Singleton
    public @NotNull Screen primaryScreen() {
        return Screen.getPrimary();
    }

    @Context
    public @NotNull ImageViewer imageViewer(
        @NotNull Stage rootWindow,
        @NotNull ImageRotationService imageRotationService,
        @NotNull RenderConfig config,
        @NotNull ScheduledExecutorService scheduledExecutorService
    ) {

        var viewer = ImageViewer.builder()
            .imageMode(config.getImageMode())
            .rotation(config.getRotation())
            .stage(rootWindow)
            .renderHeight(config.getHeight())
            .renderWidth(config.getWidth())
            .imageRotationService(imageRotationService)
            .build();

        viewer.initializeAndLoad();

        log.info(
            """
            Start image viewer with settings: {
                switch-interval: {},
                image-mode: "{}",
                rotation: "{}",
                with: "{}",
                height: "{}"
            }""",
            config.getSwitchInterval(),
            config.getImageMode(),
            config.getRotation(),
            config.getWidth(),
            config.getHeight()
        );

        scheduledExecutorService.scheduleWithFixedDelay(
            viewer::loadNext,
            config.getSwitchInterval(),
            config.getSwitchInterval(),
            TimeUnit.SECONDS
        );

        return viewer;
    }
}
