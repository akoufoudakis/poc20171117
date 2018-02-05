package org.kie.dmn.temp.poc20171117;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestKJars {

    @Test
    public void testSingle() {
        KieServices ks = KieServices.Factory.get();
        ReleaseId release = ks.newReleaseId("org.kie.dmn", "poc20171117", "1.0");
        KieFileSystem kfs = ks.newKieFileSystem().generateAndWritePomXML(release);


        KieModuleModel kmm = ks.newKieModuleModel();
        kmm.setConfigurationProperty("org.kie.dmn.profiles.poc20171117", "org.kie.dmn.temp.poc20171117.POC20171117DMNProfile");

        kfs.writeKModuleXML(kmm.toXML());

        Resource resourceCaller = ks.getResources().newClassPathResource("caller PN.dmn",
                TestKJars.class);
        kfs.write(resourceCaller);

        Resource resourceCalled = ks.getResources().newClassPathResource("called P.dmn",
                TestKJars.class);
        kfs.write(resourceCalled);

        KieBuilder kieBuilder = ks.newKieBuilder( kfs ).buildAll();
        Results results = kieBuilder.getResults();

        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(results.getMessages(Message.Level.ERROR).toString());
        }

        InternalKieModule kieModule = (InternalKieModule) ks.getRepository()
                .getKieModule(release);

        byte[] jar = kieModule.getBytes();

        Resource jarRes = ks.getResources().newByteArrayResource(jar);
        ks.getRepository().addKieModule(jarRes);

        KieContainer container = ks.newKieContainer(release);
        DMNRuntime dmnRuntime = container.newKieSession().getKieRuntime(DMNRuntime.class);

        DMNModel dmnModel = dmnRuntime.getModel("http://www.trisotech.com/definitions/_337d15f6-09db-4d20-b49e-2ce272ac0f8b",
                "caller PN");

        DMNContext dmnContext = dmnRuntime.newContext();
        dmnContext.set("a number", 47);
        dmnContext.set("abc", "value-of-abc");
        dmnContext.set("xyz", "this-is-xyz");


        DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, dmnContext);
        System.out.println(dmnResult);
        assertEquals("I am a model that depends on abc: value-of-abc and xyz: this-is-xyz and I take a decision.",
                dmnResult.getDecisionResultByName("invoke external model").getResult());

    }

    @Test
    public void testMultiple() {

        //Building the first KJar (caller)
        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseCaller = ks.newReleaseId("org.kie.dmn", "poc20171117-caller", "1.0");
        KieFileSystem kfsCaller = ks.newKieFileSystem().generateAndWritePomXML(releaseCaller);


        KieModuleModel kmm = ks.newKieModuleModel();
        kmm.setConfigurationProperty("org.kie.dmn.profiles.poc20171117", "org.kie.dmn.temp.poc20171117.POC20171117DMNProfile");

        Resource resourceCaller = ks.getResources().newClassPathResource("caller PN.dmn",
                TestKJars.class);
        kfsCaller.write(resourceCaller);

        KieBuilder kieBuilder = ks.newKieBuilder(kfsCaller).buildAll();
        Results results = kieBuilder.getResults();

        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(results.getMessages(Message.Level.ERROR).toString());
        }

        InternalKieModule kieModuleCaller = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseCaller);

        byte[] jarCaller = kieModuleCaller.getBytes();

        Resource jarResCaller = ks.getResources().newByteArrayResource(jarCaller);
        ks.getRepository().addKieModule(jarResCaller);

        //Building the second KJar (called)
        ReleaseId releaseCalled = ks.newReleaseId("org.kie.dmn", "poc20171117-called", "1.0");
        KieFileSystem kfsCalled = ks.newKieFileSystem();
        kfsCalled.writePomXML(getPomCaller());

        Resource resourceCalled = ks.getResources().newClassPathResource("called P.dmn",
                TestKJars.class);
        kfsCalled.write(resourceCalled);

        KieBuilder kieBuilderCalled = ks.newKieBuilder(kfsCalled);
        kieBuilderCalled.setDependencies(kieModuleCaller);
        kieBuilderCalled.buildAll();

        Results resultsCalled = kieBuilderCalled.getResults();

        if (resultsCalled.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(resultsCalled.getMessages(Message.Level.ERROR).toString());
        }

        InternalKieModule kieModuleCalled = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseCalled);

        byte[] jarCalled = kieModuleCalled.getBytes();

        Resource jarResCalled = ks.getResources().newByteArrayResource(jarCalled);
        ks.getRepository().addKieModule(jarResCalled);

        //Initializing a container for the "called" KJar

        KieContainer container = ks.newKieContainer(releaseCalled);
        DMNRuntime dmnRuntime = container.newKieSession().getKieRuntime(DMNRuntime.class);

        DMNModel dmnModel = dmnRuntime.getModel("http://www.trisotech.com/definitions/_337d15f6-09db-4d20-b49e-2ce272ac0f8b",
                "caller PN");

        assertNotNull(dmnModel);

        DMNContext dmnContext = dmnRuntime.newContext();
        dmnContext.set("a number", 47);
        dmnContext.set("abc", "value-of-abc");
        dmnContext.set("xyz", "this-is-xyz");


        DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, dmnContext);
        System.out.println(dmnResult);
        assertEquals("I am a model that depends on abc: value-of-abc and xyz: this-is-xyz and I take a decision.",
                dmnResult.getDecisionResultByName("invoke external model").getResult());

    }

    private String getPomCaller() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>org.kie.dmn</groupId>\n" +
                "    <artifactId>poc20171117-called</artifactId>\n" +
                "    <version>1.0</version>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.kie.dmn</groupId>\n" +
                "            <artifactId>poc20171117-caller</artifactId>\n" +
                "            <version>1.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>" +
                "</project>";
    }

}
