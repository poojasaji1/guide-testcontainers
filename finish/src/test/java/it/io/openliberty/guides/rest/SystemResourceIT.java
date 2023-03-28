// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.guides.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import io.openliberty.guides.rest.model.SystemData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// tag::testcontainers-annotation[]
@Testcontainers
// end::testcontainers-annotation[]
@TestMethodOrder(OrderAnnotation.class)
public class SystemResourceIT {

    // tag::logger[]
    private static Logger logger = LoggerFactory.getLogger(SystemResourceIT.class);
    // end::logger[]
    private static String appPath = "/inventory/api";
    private static String appImageName = "testcontainers:1.0-SNAPSHOT";

    // tag::SystemResourceClient[]
    public static SystemResourceClient client;
    // end::SystemResourceClient[]

    // tag::libertyContainer[]
    // tag::container-annotation[]
    @Container
    // end::container-annotation[]
    public static LibertyContainer libertyContainer
        = new LibertyContainer(appImageName)
              // tag::waitingFor[]
              .waitingFor(Wait.forHttp(appPath + "/systems"))
              // end::waitingFor[]
              // tag::withLogConsumer[]
              .withLogConsumer(new Slf4jLogConsumer(logger));
              // end::withLogConsumer[]
    // end::libertyContainer[]

    // tag::setupTestClass[]
    @BeforeAll
    public static void setupTestClass() throws Exception {
        System.out.println("TEST: Starting Liberty Container setup");
        client = libertyContainer.createRestClient(
            SystemResourceClient.class, appPath);
    }
    // end::setupTestClass[]

    // tag::showSystemData[]
    private void showSystemData(SystemData system) {
        System.out.println("TEST: SystemData > "
            + system.getId() + ", "
            + system.getHostname() + ", "
            + system.getOsName() + ", "
            + system.getJavaVersion() + ", "
            + system.getHeapSize());
    }
    // end::showSystemData[]

    // tag::testcases[]
    @Test
    @Order(1)
    public void testAddSystem() {
        System.out.println("TEST: Testing add a system");
        client.addSystem("localhost", "linux", "11", Long.valueOf(2048));
        List<SystemData> systems = client.listContents();
        assertEquals(1, systems.size());
        showSystemData(systems.get(0));
        assertEquals("11", systems.get(0).getJavaVersion());
        assertEquals(Long.valueOf(2048), systems.get(0).getHeapSize());
    }

    @Test
    @Order(2)
    public void testUpdateSystem() {
        System.out.println("TEST: Testing update a system");
        client.updateSystem("localhost", "linux", "8", Long.valueOf(1024));
        SystemData system = client.getSystem("localhost");
        showSystemData(system);
        assertEquals("8", system.getJavaVersion());
        assertEquals(Long.valueOf(1024), system.getHeapSize());
    }

    @Test
    @Order(3)
    public void testRemoveSystem() {
        System.out.println("TEST: Testing remove a system");
        client.removeSystem("localhost");
        List<SystemData> systems = client.listContents();
        assertEquals(0, systems.size());
    }
    // end::testcases[]
}