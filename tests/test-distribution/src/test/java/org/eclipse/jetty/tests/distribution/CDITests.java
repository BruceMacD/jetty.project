//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.tests.distribution;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CDITests extends AbstractDistributionTest
{
    // Tests from here use these parameters
    public static Stream<Arguments> tests()
    {
        List<Object[]> tests = new ArrayList<>();

        // TODO tests.add(new Object[]{"weld", "cdi"}); // Requires Weld >= 3.1.2 ???
        tests.add(new Object[]{"weld", "cdi2"});
        // TODO tests.add(new Object[]{"weld", "decorate"}); // Requires Weld >= 3.1.2
        tests.add(new Object[]{"owb", "cdi"});
        tests.add(new Object[]{"owb", "cdi2"});
        // tests.add(new Object[]{"owb", "decorate"});  // Probably will not be supported
        return tests.stream().map(Arguments::of);
    }

    /**
     * Tests a WAR file that includes the CDI
     * library in its WEB-INF/lib directory.
     */
    @ParameterizedTest
    @MethodSource("tests")
    public void testCDIIncludedInWebapp(String implementation, String integration) throws Exception
    {
        String jettyVersion = System.getProperty("jettyVersion");
        DistributionTester distribution = DistributionTester.Builder.newInstance()
            .jettyVersion(jettyVersion)
            .mavenLocalRepository(System.getProperty("mavenRepoPath"))
            .build();

        String[] args1 = {
            "--create-startd",
            "--approve-all-licenses",
            "--add-to-start=http,deploy,annotations,jsp,"+integration
        };
        try (DistributionTester.Run run1 = distribution.start(args1))
        {
            assertTrue(run1.awaitFor(5, TimeUnit.SECONDS));
            assertEquals(0, run1.getExitValue());

            File war = distribution.resolveArtifact("org.eclipse.jetty.tests:test-" + implementation + "-cdi-webapp:war:" + jettyVersion);
            distribution.installWarFile(war, "demo");

            int port = distribution.freePort();
            try (DistributionTester.Run run2 = distribution.start("jetty.http.port=" + port))
            {
                assertTrue(run2.awaitConsoleLogsFor("Started @", 10, TimeUnit.SECONDS));

                startHttpClient();
                ContentResponse response = client.GET("http://localhost:" + port + "/demo/greetings");
                assertEquals(HttpStatus.OK_200, response.getStatus());
                // Confirm Servlet based CDI
                assertThat(response.getContentAsString(), containsString("Hello GreetingsServlet"));
                // Confirm Listener based CDI (this has been a problem in the past, keep this for regression testing!)
                assertThat(response.getHeaders().get("Server"), containsString("CDI-Demo-org.eclipse.jetty.test"));

                run2.stop();
                assertTrue(run2.awaitFor(5, TimeUnit.SECONDS));
            }
        }
    }
}
