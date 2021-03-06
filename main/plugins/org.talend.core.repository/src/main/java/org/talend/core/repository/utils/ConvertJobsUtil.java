// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.repository.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.swt.custom.CCombo;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.CommonExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.MessageBoxExceptionHandler;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.hadoop.HadoopConstants;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.ui.ITestContainerProviderService;
import org.talend.designer.core.convert.IProcessConvertService;
import org.talend.designer.core.convert.IProcessConvertToAllTypeService;
import org.talend.designer.core.convert.ProcessConvertManager;
import org.talend.designer.core.convert.ProcessConverterType;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;

/**
 * created by hcyi on May 25, 2015 Detailled comment
 *
 */
public class ConvertJobsUtil {

    public static final String FRAMEWORK = HadoopConstants.FRAMEWORK;

    public static final String STORM_FRAMEWORK = HadoopConstants.FRAMEWORK_STORM;

    public static final String SPARKSTREAMING_FRAMEWORK = HadoopConstants.FRAMEWORK_SPARKSTREAMING;

    public static final String MAPREDUCE_FRAMEWORK = HadoopConstants.FRAMEWORK_MAPREDUCE;

    public static final String SPARK_FRAMEWORK = HadoopConstants.FRAMEWORK_SPARK;

    public static enum JobType {
        STANDARD("Standard", "_STANDARD_", ERepositoryObjectType.PROCESS), //$NON-NLS-1$ //$NON-NLS-2$
        BIGDATASTREAMING("Big Data Streaming", "_BIG_DATA_STREAMING_", ERepositoryObjectType.PROCESS_STORM), //$NON-NLS-1$ //$NON-NLS-2$
        BIGDATABATCH("Big Data Batch", "_BIG_DATA_BATCH_", ERepositoryObjectType.PROCESS_MR); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        private ERepositoryObjectType repositoryObjectType;

        JobType(String displayName, String fileName, ERepositoryObjectType repositoryObjectType) {
            this.displayName = displayName;
            this.fileName = fileName;
            this.repositoryObjectType = repositoryObjectType;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public ERepositoryObjectType getERepositoryObjectType() {
            return this.repositoryObjectType;
        }

        public static String[] getJobTypeToDispaly() {
            String[] dispalyNames = new String[values().length];
            List<String> dispalyNamesList = new ArrayList<String>();
            for (int i = 0; i < values().length; i++) {
                dispalyNamesList.add(i, values()[i].getDisplayName());
            }
            if (!PluginChecker.isStormPluginLoader()) {
                dispalyNamesList.remove(JobType.BIGDATASTREAMING.getDisplayName());
            }
            if (!PluginChecker.isMapReducePluginLoader()) {
                dispalyNamesList.remove(JobType.BIGDATABATCH.getDisplayName());
            }
            dispalyNames = new String[dispalyNamesList.size()];
            for (int j = 0; j < dispalyNamesList.size(); j++) {
                if (dispalyNamesList.get(j) != null) {
                    dispalyNames[j] = dispalyNamesList.get(j);
                }
            }
            return dispalyNames;
        }

        public static JobType getJobTypeByDisplayName(String displayName) {
            if (displayName == null) {
                return null;
            }
            JobType jobTypes[] = JobType.values();
            if (jobTypes == null) {
                return null;
            }
            for (JobType jobType : jobTypes) {
                if (jobType.getDisplayName().equals(displayName)) {
                    return jobType;
                }
            }
            return null;
        }

    }

    public static enum JobStreamingFramework {
        STORMFRAMEWORK("Storm", "_STORM_STORM_FRAMEWORK_"), //$NON-NLS-1$ //$NON-NLS-2$
        SPARKSTREAMINGFRAMEWORK("Spark Streaming", "_STORM_SPARKSTREAMING_FRAMEWORK_"); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        JobStreamingFramework(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public static String[] getFrameworkToDispaly() {
            String[] dispalyNames = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                dispalyNames[i] = values()[i].getDisplayName();
            }
            return dispalyNames;
        }
    }

    public static enum JobBatchFramework {
        MAPREDUCEFRAMEWORK("MapReduce", "_MAPREDUCE_FRAMEWORK_"), //$NON-NLS-1$ //$NON-NLS-2$
        SPARKFRAMEWORK("Spark", "_SPARK_FRAMEWORK_"); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        JobBatchFramework(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public static String[] getFrameworkToDispaly() {
            String[] dispalyNames = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                dispalyNames[i] = values()[i].getDisplayName();
            }
            return dispalyNames;
        }
    }

    public static enum Status {
        DEVELOPMENT("development", "_DEVELOPMENT_"), //$NON-NLS-1$ //$NON-NLS-2$
        TESTING("testing", "_TESTING_"), //$NON-NLS-1$ //$NON-NLS-2$
        PRODUCTION("production", "_PRODUCTION_"); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        Status(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public static String[] getStatusToDispaly() {
            String[] dispalyNames = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                dispalyNames[i] = values()[i].getDisplayName();
            }
            return dispalyNames;
        }
    }

    public static void updateJobFrameworkPart(String jobTypeValue, CCombo frameworkCombo) {
        frameworkCombo.setEnabled(true);
        if (JobType.STANDARD.getDisplayName().equals(jobTypeValue)) {
            frameworkCombo.setItems(new String[0]);
            frameworkCombo.setText("");//$NON-NLS-1$ 
            frameworkCombo.setEnabled(false);
        } else if (JobType.BIGDATABATCH.getDisplayName().equals(jobTypeValue)) {
            String[] items = JobBatchFramework.getFrameworkToDispaly();
            frameworkCombo.setItems(items);
            if (items.length > 0) {
                frameworkCombo.select(0);
            }
        } else if (JobType.BIGDATASTREAMING.getDisplayName().equals(jobTypeValue)) {
            String[] items = JobStreamingFramework.getFrameworkToDispaly();
            frameworkCombo.setItems(items);
            if (items.length > 0) {
                frameworkCombo.select(0);
            }
        }
    }

    /**
     * get the target execution framework from the field in Job properties
     * 
     * @param item
     * @return
     */
    public static String getFramework(Item item) {
        if (item != null) {
            Property property = item.getProperty();
            if (property != null && property.getAdditionalProperties() != null
                    && property.getAdditionalProperties().containsKey(FRAMEWORK)) {
                return (String) property.getAdditionalProperties().get(FRAMEWORK);
            }
        }
        return null;
    }

    public static String getJobTypeFromFramework(Item item) {
        String frameworkObj = ConvertJobsUtil.getFramework(item);
        return getJobTypeFromFramework(frameworkObj);
    }

    /**
     * DOC nrousseau Comment method "getJobTypeFromFramework".
     * 
     * @param frameworkObj
     * @return
     */
    public static String getJobTypeFromFramework(String frameworkObj) {
        if (JobBatchFramework.MAPREDUCEFRAMEWORK.getDisplayName().equals(frameworkObj)
                || JobBatchFramework.SPARKFRAMEWORK.getDisplayName().equals(frameworkObj)) {
            return JobType.BIGDATABATCH.getDisplayName();
        } else if (JobStreamingFramework.STORMFRAMEWORK.getDisplayName().equals(frameworkObj)
                || JobStreamingFramework.SPARKSTREAMINGFRAMEWORK.getDisplayName().equals(frameworkObj)) {
            return JobType.BIGDATASTREAMING.getDisplayName();
        } else {
            return JobType.STANDARD.getDisplayName();
        }
    }

    public static String[] getFrameworkItemsByJobType(String jobType) {
        if (JobType.BIGDATABATCH.getDisplayName().equals(jobType)) {
            return JobBatchFramework.getFrameworkToDispaly();
        } else if (JobType.BIGDATASTREAMING.getDisplayName().equals(jobType)) {
            return JobStreamingFramework.getFrameworkToDispaly();
        } else {
            return new String[0];
        }
    }

    public static boolean isNeedConvert(String oldJobTypeValue, String oldFrameworkValue, String newJobTypeValue,
            String newFrameworkValue) {
        JobType oldJobType = JobType.getJobTypeByDisplayName(oldJobTypeValue);
        JobType newJobType = JobType.getJobTypeByDisplayName(newJobTypeValue);
        ERepositoryObjectType oldRepType = (oldJobType == null ? null : oldJobType.getERepositoryObjectType());
        ERepositoryObjectType newRepType = (newJobType == null ? null : newJobType.getERepositoryObjectType());
        return ProcessConvertManager.getInstance().CheckConvertProcess(oldRepType, oldFrameworkValue, newRepType,
                newFrameworkValue);
    }

    public static boolean hasTestCase(Property property) {
        boolean hasTestCase = false;
        if (GlobalServiceRegister.getDefault().isServiceRegistered(ITestContainerProviderService.class)) {
            ITestContainerProviderService testContainerService = (ITestContainerProviderService) GlobalServiceRegister
                    .getDefault().getService(ITestContainerProviderService.class);
            if (testContainerService != null) {
                hasTestCase = testContainerService.hasTestCase(property);
            }
        }
        return hasTestCase;
    }

    public static Item createOperation(final String newJobName, final String jobTypeValue, final String frameworkValue,
            final IRepositoryViewObject sourceObject) {
        IProcessConvertService converter = null;
        if (sourceObject == null || sourceObject.getProperty() == null || newJobName == null) {
            return null;
        }
        Item item = sourceObject.getProperty().getItem();
        if (JobType.STANDARD.getDisplayName().equals(jobTypeValue)) {
            String sourceJobType = getJobTypeFromFramework(item);
            if (JobType.BIGDATASTREAMING.getDisplayName().equals(sourceJobType)
                    || ERepositoryObjectType.PROCESS_STORM == sourceObject.getRepositoryObjectType()) {
                converter = ProcessConvertManager.getInstance().extractConvertService(ProcessConverterType.CONVERTER_FOR_STORM);
            } else if (JobType.BIGDATABATCH.getDisplayName().equals(sourceJobType)
                    || ERepositoryObjectType.PROCESS_MR == sourceObject.getRepositoryObjectType()) {
                converter = ProcessConvertManager.getInstance().extractConvertService(
                        ProcessConverterType.CONVERTER_FOR_MAPREDUCE);
            }
            if (converter != null && converter instanceof IProcessConvertToAllTypeService) {
                return ((IProcessConvertToAllTypeService) converter).convertToProcess(item, sourceObject, newJobName,
                        jobTypeValue);
            }
        } else if (JobType.BIGDATASTREAMING.getDisplayName().equals(jobTypeValue)) {
            converter = ProcessConvertManager.getInstance().extractConvertService(ProcessConverterType.CONVERTER_FOR_STORM);
            if (converter != null && converter instanceof IProcessConvertToAllTypeService) {
                return ((IProcessConvertToAllTypeService) converter).convertToProcessStreaming(item, sourceObject, newJobName,
                        jobTypeValue, frameworkValue);
            }
        } else if (JobType.BIGDATABATCH.getDisplayName().equals(jobTypeValue)) {
            converter = ProcessConvertManager.getInstance().extractConvertService(ProcessConverterType.CONVERTER_FOR_MAPREDUCE);
            if (converter != null && converter instanceof IProcessConvertToAllTypeService) {
                return ((IProcessConvertToAllTypeService) converter).convertToProcessBatch(item, sourceObject, newJobName,
                        jobTypeValue, frameworkValue);
            }
        }
        return null;
    }

    public static boolean convert(String newJobName, String jobTypeValue, String frameworkValue,
            final IRepositoryViewObject sourceObject) {
        final Item newItem = ConvertJobsUtil.createOperation(newJobName, jobTypeValue, frameworkValue, sourceObject);
        if (newItem == null) {
            return false;
        }

        boolean isNewItemCreated = true;
        Property repositoryProperty = sourceObject.getProperty();
        if (repositoryProperty != null) {
            isNewItemCreated = (repositoryProperty.getItem() != newItem);
        }

        if (isNewItemCreated) {
            // delete the old item
            IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

                @Override
                public void run(final IProgressMonitor monitor) throws CoreException {
                    IProxyRepositoryFactory proxyRepositoryFactory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
                    try {
                        proxyRepositoryFactory.unlock(sourceObject);
                        proxyRepositoryFactory.deleteObjectPhysical(sourceObject);
                        proxyRepositoryFactory.saveProject(ProjectManager.getInstance().getCurrentProject());
                    } catch (PersistenceException e1) {
                        CommonExceptionHandler.process(e1);
                    } catch (LoginException e) {
                        CommonExceptionHandler.process(e);
                    }
                }
            };
            // unlockObject();
            // alreadyEditedByUser = true; // to avoid 2 calls of unlock

            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            try {
                ISchedulingRule schedulingRule = workspace.getRoot();
                // the update the project files need to be done in the workspace runnable to avoid all notification
                // of changes before the end of the modifications.
                workspace.run(runnable, schedulingRule, IWorkspace.AVOID_UPDATE, null);
                return true;
            } catch (CoreException e) {
                MessageBoxExceptionHandler.process(e.getCause());
                return false;
            }
        }
        return true;
    }

    /**
     * DOC nrousseau Comment method "getFrameworkItemsByJobType".
     * 
     * @param repositoryObjectType
     * @return
     */
    public static String[] getFrameworkItemsByJobType(ERepositoryObjectType repositoryObjectType) {
        if (repositoryObjectType != null) {
            if (repositoryObjectType.equals(ERepositoryObjectType.PROCESS_MR)) {
                return JobBatchFramework.getFrameworkToDispaly();
            } else if (repositoryObjectType.equals(ERepositoryObjectType.PROCESS_STORM)) {
                return JobStreamingFramework.getFrameworkToDispaly();
            }
        }
        return new String[0];
    }

    /**
     * Update framework if change it when duplicating DOC Comment method "updateFramework".
     * 
     * @param item
     * @param newFrameworkNewValue
     */
    public static void updateFramework(Item item, String frameworkNewValue) {
        if (item instanceof ProcessItem) {
            Property newProperty = item.getProperty();
            if (newProperty != null && newProperty.getAdditionalProperties() != null
                    && newProperty.getAdditionalProperties().containsKey(ConvertJobsUtil.FRAMEWORK)) {
                String currentFramework = (String) newProperty.getAdditionalProperties().get(ConvertJobsUtil.FRAMEWORK);
                if (currentFramework != null && frameworkNewValue != null && !currentFramework.equals(frameworkNewValue)) {
                    newProperty.getAdditionalProperties().put(ConvertJobsUtil.FRAMEWORK, frameworkNewValue);
                }
            }
        }
    }

    public static String getDuplicateName(RepositoryNode node, String value) throws BusinessException {
        char j = 'a';
        String temp = value + "_a";//$NON-NLS-1$
        while (validJobName(node, temp)) {
            if (j > 'z') {
                //
            }
            temp = value + "_" + (j++) + ""; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return temp;
    }

    public static boolean validJobName(RepositoryNode node, String itemName) {
        IProxyRepositoryFactory proxyRepositoryFactory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
        try {
            List<IRepositoryViewObject> listExistingObjects = proxyRepositoryFactory.getAll(ERepositoryObjectType.PROCESS, true,
                    false);
            if (PluginChecker.isStormPluginLoader()) {
                listExistingObjects.addAll(proxyRepositoryFactory.getAll(ERepositoryObjectType.PROCESS_STORM, true, false));
            }
            if (PluginChecker.isMapReducePluginLoader()) {
                listExistingObjects.addAll(proxyRepositoryFactory.getAll(ERepositoryObjectType.PROCESS_MR, true, false));
            }
            if (!proxyRepositoryFactory.isNameAvailable(node.getObject().getProperty().getItem(), itemName, listExistingObjects)) {
                return true;
            }
        } catch (PersistenceException e1) {
            return true;
        }
        return false;
    }
}
