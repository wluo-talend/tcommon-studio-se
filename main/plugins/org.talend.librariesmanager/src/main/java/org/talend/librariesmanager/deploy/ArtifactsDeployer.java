// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.librariesmanager.deploy;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.nexus.NexusConstants;
import org.talend.core.nexus.NexusServerBean;
import org.talend.core.nexus.NexusServerManager;
import org.talend.core.runtime.maven.MavenArtifact;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.tools.repo.LocalRepsitoryLauncherManager;
import org.talend.designer.maven.utils.PomUtil;

/**
 * created by wchen on 2015-5-14 Detailled comment
 *
 */
public class ArtifactsDeployer {

    private static final String SLASH = "/";//$NON-NLS-1$ 

    private static ArtifactsDeployer deployer;

    private NexusServerBean nexusServer;

    private LocalRepsitoryLauncherManager repositoryManager;

    private ArtifactsDeployer() {
        init();
    }

    private void init() {
        nexusServer = NexusServerManager.getLibrariesNexusServer(false);
        if (nexusServer != null) {
            String server = nexusServer.getServer().trim();
            if (server.endsWith(NexusConstants.SLASH)) {
                server = server.substring(0, server.length() - 1);
            }
        }
        repositoryManager = new LocalRepsitoryLauncherManager();
    }

    public static ArtifactsDeployer getInstance() {
        if (deployer == null) {
            deployer = new ArtifactsDeployer();
        }
        return deployer;
    }

    /**
     * 
     * DOC Talend Comment method "deployToLocalMaven".
     * 
     * @param jarSourceAndMavenUri a map with key : can be a filePath or platform uri , value :maven uri
     * @throws Exception
     */
    public void deployToLocalMaven(Map<String, String> jarSourceAndMavenUri) throws Exception {
        for (String path : jarSourceAndMavenUri.keySet()) {
            deployToLocalMaven(path, jarSourceAndMavenUri.get(path));
        }
    }

    /**
     * 
     * DOC Talend Comment method "deployToLocalMaven".
     * 
     * @param uriOrPath can be a filePath or platform uri
     * @param mavenUri maven uri
     * @throws Exception
     */
    public void deployToLocalMaven(String path, String mavenUri) throws Exception {
        MavenArtifact parseMvnUrl = MavenUrlHelper.parseMvnUrl(mavenUri);
        if (parseMvnUrl != null) {
            // install to local maven repository and create pom
            repositoryManager.install(new File(path), parseMvnUrl);

            String absArtifactPath = PomUtil.getAbsArtifactPath(parseMvnUrl);
            String pomPath = null;
            String type = null;
            if (absArtifactPath != null) {
                if (absArtifactPath.lastIndexOf(".") != -1) {
                    pomPath = absArtifactPath.substring(0, absArtifactPath.lastIndexOf(".") + 1)
                            + TalendMavenConstants.PACKAGING_POM;
                    type = absArtifactPath.substring(absArtifactPath.lastIndexOf(".") + 1, absArtifactPath.length());
                } else {
                    // incase installed file do not have file extension
                    pomPath = pomPath + TalendMavenConstants.PACKAGING_POM;
                }
            }

            if (nexusServer != null && !nexusServer.isOfficial()) {
                // deploy to nexus server if it is not null and not official server
                // repositoryManager.deploy(new File(path), parseMvnUrl);
                installToRemote(new File(path), parseMvnUrl, type);
                // deploy the pom
                if (new File(pomPath).exists()) {
                    installToRemote(new File(pomPath), parseMvnUrl, TalendMavenConstants.PACKAGING_POM);
                }
            }

        }
    }

    protected void installToRemote(File content, MavenArtifact artifact, String type) throws BusinessException {
        URL targetURL;
        try {
            String artifactPath = PomUtil.getArtifactPath(artifact);
            if (!artifactPath.endsWith(type)) {
                if (artifactPath.lastIndexOf(".") != -1) {
                    artifactPath = artifactPath.substring(0, artifactPath.lastIndexOf(".") + 1) + type;
                } else {
                    artifactPath = artifactPath + "." + type;
                }
            }
            String target = nexusServer.getRepositoryUrl() + artifactPath;
            targetURL = new URL(target);
            installToRemote(new FileEntity(content), targetURL);
        } catch (MalformedURLException e) {
            ExceptionHandler.process(e);
        }
    }

    private void installToRemote(HttpEntity entity, URL targetURL) throws BusinessException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(targetURL.getHost(), targetURL.getPort()),
                    new UsernamePasswordCredentials(nexusServer.getUserName(), nexusServer.getPassword()));

            HttpPut httpPut = new HttpPut(targetURL.toString());
            httpPut.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPut);
            StatusLine statusLine = response.getStatusLine();
            int responseCode = statusLine.getStatusCode();
            EntityUtils.consume(entity);
            if (responseCode > 399) {
                if (responseCode == 401) {
                    throw new BusinessException("Authrity failed");
                } else {
                    throw new BusinessException("Deploy failed: " + responseCode + ' ' + statusLine.getReasonPhrase());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("softwareupdate.error.cannotupload", e.getMessage());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // private void install(String path, MavenArtifact artifact) {
    // StringBuffer command = new StringBuffer();
    // // mvn -Dfile=E:\studio_code\.metadata\aaabbbb\lib\java\ojdbc6.jar -DgroupId=org.talend.libraries
    // // -DartifactId=ojdbc6 -Dversion=1.0.0 -Dpackaging=jar
    // // -B install:install-file
    // command.append(" mvn ");
    // command.append(" -Dfile=");
    // command.append(path);
    // command.append(" -DgroupId=");
    // command.append(artifact.getGroupId());
    // command.append(" -DartifactId=");
    // command.append(artifact.getArtifactId());
    // command.append(" -Dversion=");
    // command.append(artifact.getVersion());
    // command.append(" -Dpackaging=");
    // command.append(artifact.getType());
    // command.append(" -B install:install-file");
    // try {
    // Runtime.getRuntime().exec("cmd /c " + command.toString());
    // } catch (IOException e) {
    // ExceptionHandler.process(e);
    // }
    // }
}