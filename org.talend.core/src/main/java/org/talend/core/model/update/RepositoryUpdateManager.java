// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.model.update;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.MultiKeyMap;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.CorePlugin;
import org.talend.core.i18n.Messages;
import org.talend.core.model.context.JobContextManager;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.builder.ConvertionHelper;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.QueriesConnection;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.repository.UpdateRepositoryUtils;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;

/**
 * ggu class global comment. Detailled comment
 */
public abstract class RepositoryUpdateManager {

    /**
     * for repository context rename.
     */
    private Map<ContextItem, Map<String, String>> repositoryRenamedMap = new HashMap<ContextItem, Map<String, String>>();

    private Map<String, String> schemaRenamedMap = new HashMap<String, String>();

    /**
     * used for filter result.
     */
    private Object parameter;

    private Map<ContextItem, Set<String>> newParametersMap = new HashMap<ContextItem, Set<String>>();

    public RepositoryUpdateManager(Object parameter) {
        super();
        this.parameter = parameter;
    }

    /*
     * context
     */
    public Map<ContextItem, Map<String, String>> getContextRenamedMap() {
        return this.repositoryRenamedMap;
    }

    public void setContextRenamedMap(Map<ContextItem, Map<String, String>> repositoryRenamedMap) {
        this.repositoryRenamedMap = repositoryRenamedMap;
    }

    /*
     * Schema old name to new one
     */

    public Map<String, String> getSchemaRenamedMap() {
        return this.schemaRenamedMap;
    }

    public void setSchemaRenamedMap(Map<String, String> schemaRenamedMap) {
        this.schemaRenamedMap = schemaRenamedMap;
    }

    public abstract Set<EUpdateItemType> getTypes();

    private boolean openPropagationDialog() {
        return MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages
                .getString("RepositoryUpdateManager.Title"), //$NON-NLS-1$
                Messages.getString("RepositoryUpdateManager.Messages")); //$NON-NLS-1$
    }

    private void openNoModificationDialog() {
        MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages
                .getString("RepositoryUpdateManager.NoModificationTitle"), //$NON-NLS-1$
                Messages.getString("RepositoryUpdateManager.NoModificationMessages")); //$NON-NLS-1$
    }

    public boolean doWork() {
        return doWork(true);
    }

    public boolean needForcePropagation() {
        return (getContextRenamedMap() != null && !getContextRenamedMap().isEmpty())
                || (getSchemaRenamedMap() != null && !getSchemaRenamedMap().isEmpty());
    }

    public boolean doWork(boolean show) {
        // check the dialog.
        boolean checked = true;
        boolean showed = false;
        if (show) {
            if (parameter != null && !needForcePropagation()) {
                // see feature 4786
                boolean deactive = Boolean.parseBoolean(CorePlugin.getDefault().getDesignerCoreService().getPreferenceStore(
                        ITalendCorePrefConstants.DEACTIVE_REPOSITORY_UPDATE));
                if (deactive) {
                    return false;
                }

                checked = openPropagationDialog();
                showed = true;
            }
        } else {
            showed = true;
        }
        if (checked) {
            final List<UpdateResult> results = new ArrayList<UpdateResult>();
            boolean cancelable = !needForcePropagation();
            IRunnableWithProgress runnable = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    List<UpdateResult> returnResult = checkJobItemsForUpdate(monitor, getTypes());
                    if (returnResult != null) {
                        results.addAll(returnResult);
                    }
                }
            };

            try {
                final ProgressMonitorJobsDialog dialog = new ProgressMonitorJobsDialog(null);
                dialog.run(true, cancelable, runnable);

                // PlatformUI.getWorkbench().getProgressService().run(true, true, runnable);
            } catch (InvocationTargetException e) {
                ExceptionHandler.process(e);
            } catch (InterruptedException e) {
                if (e.getMessage().equals(UpdatesConstants.MONITOR_IS_CANCELED)) {
                    return false;
                }
                ExceptionHandler.process(e);
            }
            List<UpdateResult> checkedResults = null;

            if (parameter == null) { // update all job
                checkedResults = results;
            } else { // filter
                checkedResults = filterCheckedResult(results);
            }
            if (checkedResults != null && !checkedResults.isEmpty()) {
                if (showed || parameter == null || unShowDialog(checkedResults) || openPropagationDialog()) {
                    IDesignerCoreService designerCoreService = CorePlugin.getDefault().getDesignerCoreService();
                    return designerCoreService.executeUpdatesManager(checkedResults);
                }
                return false;
            }
            openNoModificationDialog();
        }
        return false;
    }

    private List<UpdateResult> filterCheckedResult(List<UpdateResult> results) {
        if (results == null) {
            return null;
        }
        List<UpdateResult> checkedResults = new ArrayList<UpdateResult>();
        for (UpdateResult result : results) {
            if (filterForType(result)) {
                checkedResults.add(result);
            } else {
                // for context
                if (result.getUpdateType() == EUpdateItemType.CONTEXT && result.getResultType() == EUpdateResult.BUIL_IN) {
                    checkedResults.add(result);
                }
            }

        }
        return checkedResults;
    }

    private boolean unShowDialog(List<UpdateResult> checkedResults) {
        if (checkedResults == null) {
            return false;
        }
        for (UpdateResult result : checkedResults) {
            if (result.getResultType() != EUpdateResult.UPDATE) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean filterForType(UpdateResult result) {
        if (result == null || parameter == null) {
            return false;
        }
        Object object = result.getParameter();
        if (object == null) {
            return false;
        }
        if (object == parameter) {
            return true;
        }
        if (object instanceof List) {
            List list = ((List) object);
            if (!list.isEmpty()) {
                Object firstObj = list.get(0);
                if (parameter == firstObj) { // for context rename
                    return true;
                }

                // schema
                if (checkResultSchema(result, firstObj, parameter)) {
                    return true;
                }

            }

        }
        // schema
        if (checkResultSchema(result, object, parameter)) {
            return true;
        }
        // query for wizard
        if (parameter instanceof QueriesConnection && object instanceof Query) {
            for (Query query : (List<Query>) ((QueriesConnection) parameter).getQuery()) {
                if (query.getId().equals(((Query) object).getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkResultSchema(UpdateResult result, Object object, Object parameter) {
        if (object == null || parameter == null) {
            return false;
        }
        // schema
        if (object instanceof IMetadataTable) { // 
            if (parameter instanceof ConnectionItem) { //
                String source = UpdateRepositoryUtils.getRepositorySourceName((ConnectionItem) parameter);
                if (result.getRemark() != null && result.getRemark().startsWith(source)) {
                    return true;
                }
            } else if (parameter instanceof org.talend.core.model.metadata.builder.connection.MetadataTable) {
                IMetadataTable table1 = ((IMetadataTable) object);
                MetadataTable table2 = (org.talend.core.model.metadata.builder.connection.MetadataTable) parameter;
                if (table1.getId() == null || table2.getId() == null) {
                    return table1.getLabel().equals(table2.getLabel());
                } else {
                    return table1.getId().equals(table2.getId());
                }
            }
        }
        return false;
    }

    public static IEditorReference[] getEditors() {
        final List<IEditorReference> list = new ArrayList<IEditorReference>();
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                IEditorReference[] reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                        .getEditorReferences();
                list.addAll(Arrays.asList(reference));
            }
        });
        return list.toArray(new IEditorReference[0]);
    }

    /**
     * 
     * ggu Comment method "checkJobItemsForUpdate".
     * 
     * @param types - need update types of jobs.
     * @param sourceIdMap - map old source id to new one.
     * @param sourceItem - modified repository item.
     * @return
     */
    private List<UpdateResult> checkJobItemsForUpdate(IProgressMonitor parentMonitor, final Set<EUpdateItemType> types)
            throws InterruptedException {
        if (types == null || types.isEmpty()) {
            return null;
        }

        final List<IEditorReference> list = new ArrayList<IEditorReference>();
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                IEditorReference[] reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                        .getEditorReferences();
                list.addAll(Arrays.asList(reference));
            }
        });

        List<IProcess> openedProcessList = CorePlugin.getDefault().getDesignerCoreService().getOpenedProcess(getEditors());

        try {
            List<UpdateResult> resultList = new ArrayList<UpdateResult>();
            //
            IProxyRepositoryFactory factory = CorePlugin.getDefault().getProxyRepositoryFactory();
            List<IRepositoryObject> processList = factory.getAll(ERepositoryObjectType.PROCESS, true);
            if (processList == null) {
                processList = new ArrayList<IRepositoryObject>();
            }
            List<IRepositoryObject> jobletList = factory.getAll(ERepositoryObjectType.JOBLET, true);
            if (jobletList != null) {
                processList.addAll(jobletList);
            }
            // must match TalendDesignerPrefConstants.CHECK_ONLY_LAST_VERSION
            boolean checkOnlyLastVersion = Boolean.parseBoolean(CorePlugin.getDefault().getDesignerCoreService()
                    .getPreferenceStore("checkOnlyLastVersion"));
            // get all version
            List<IRepositoryObject> allVersionList = new ArrayList<IRepositoryObject>((int) (processList.size() * 1.1));
            for (IRepositoryObject repositoryObj : processList) {
                if (!checkOnlyLastVersion) {
                    List<IRepositoryObject> allVersion = factory.getAllVersion(repositoryObj.getId());
                    for (IRepositoryObject object : allVersion) {
                        if (factory.getStatus(object) != ERepositoryStatus.LOCK_BY_OTHER
                                && factory.getStatus(object) != ERepositoryStatus.LOCK_BY_USER) {
                            allVersionList.add(object);
                        }
                    }
                } else {
                    // assume that repositoryObj is the last version, otherwise we should call
                    // factory.getLastVersion(repositoryObj.getId());
                    IRepositoryObject lastVersion = repositoryObj; // factory.getLastVersion(repositoryObj.getId());
                    if (factory.getStatus(lastVersion) != ERepositoryStatus.LOCK_BY_OTHER
                            && factory.getStatus(lastVersion) != ERepositoryStatus.LOCK_BY_USER) {
                        allVersionList.add(lastVersion);
                    }
                }
            }
            //
            int size = (allVersionList.size() + openedProcessList.size() + 1) * UpdatesConstants.SCALE;
            parentMonitor.beginTask(Messages.getString("RepositoryUpdateManager.Check"), size); //$NON-NLS-1$
            checkMonitorCanceled(parentMonitor);
            MultiKeyMap openProcessMap = createOpenProcessMap(openedProcessList);

            for (IRepositoryObject repositoryObj : allVersionList) {
                checkMonitorCanceled(parentMonitor);
                Item item = repositoryObj.getProperty().getItem();
                // avoid the opened job
                if (isOpenedItem(item, openProcessMap)) {
                    continue;
                }
                List<UpdateResult> updatesNeededFromItems = getUpdatesNeededFromItems(parentMonitor, item, types);
                if (updatesNeededFromItems != null) {
                    resultList.addAll(updatesNeededFromItems);
                }
            }

            // opened job
            for (IProcess process : openedProcessList) {
                checkMonitorCanceled(parentMonitor);
                List<UpdateResult> resultFromProcess = getResultFromProcess(parentMonitor, process, types);
                if (resultFromProcess != null) {
                    resultList.addAll(resultFromProcess);
                }
            }

            // Ok, you also need to update the job setting in "create job with template"
            List<UpdateResult> templateSetUpdate = checkSettingInJobTemplateWizard();
            if (templateSetUpdate != null) {
                resultList.addAll(templateSetUpdate);
            }

            parentMonitor.done();
            return resultList;
        } catch (PersistenceException e) {
            //
        }

        return null;
    }

    private void checkMonitorCanceled(IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException(UpdatesConstants.MONITOR_IS_CANCELED);
        }
    }

    /**
     * DOC YeXiaowei Comment method "checkSettingInJobTemplateWizard".
     */
    private List<UpdateResult> checkSettingInJobTemplateWizard() {
        List<IProcess> processes = CorePlugin.getDefault().getDesignerCoreService().getProcessForJobTemplate();

        if (processes == null || processes.isEmpty()) {
            return null;
        }

        List<UpdateResult> result = new ArrayList<UpdateResult>();

        for (IProcess process : processes) {
            if (process instanceof IProcess2) {
                IProcess2 nowProcess = (IProcess2) process;
                nowProcess.getUpdateManager().checkAllModification();
                List<UpdateResult> results = nowProcess.getUpdateManager().getUpdatesNeeded();
                if (results != null) {
                    result.addAll(results);
                }
            }
        }

        return result;
    }

    /**
     * Create a hashmap for fash lookup of the specified IProcess.
     * 
     * @param openedProcessList
     * @return
     */
    private MultiKeyMap createOpenProcessMap(List<IProcess> openedProcessList) {
        MultiKeyMap map = new MultiKeyMap();
        if (openedProcessList != null) {
            for (IProcess process : openedProcessList) {
                map.put(process.getId(), process.getLabel(), process.getVersion(), process);
            }
        }
        return map;
    }

    private boolean isOpenedItem(Item openedItem, MultiKeyMap openProcessMap) {
        if (openedItem == null) {
            return false;
        }
        Property property = openedItem.getProperty();
        return (openProcessMap.get(property.getId(), property.getLabel(), property.getVersion()) != null);
    }

    private List<UpdateResult> getResultFromProcess(IProgressMonitor parentMonitor, IProcess process,
            final Set<EUpdateItemType> types) {
        if (process == null || types == null) {
            return null;
        }
        if (parentMonitor == null) {
            parentMonitor = new NullProgressMonitor();
        }
        SubProgressMonitor subMonitor = new SubProgressMonitor(parentMonitor, 1 * UpdatesConstants.SCALE,
                SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
        subMonitor.beginTask(UpdatesConstants.EMPTY, types.size());
        subMonitor.subTask(getUpdateJobInfor(process));

        List<UpdateResult> resultList = new ArrayList<UpdateResult>();
        if (process instanceof IProcess2) {
            IProcess2 process2 = (IProcess2) process;
            // context rename
            IContextManager contextManager = process2.getContextManager();
            if (contextManager instanceof JobContextManager) {
                JobContextManager jobContextManager = (JobContextManager) contextManager;
                jobContextManager.setRepositoryRenamedMap(getContextRenamedMap());
                jobContextManager.setNewParametersMap(getNewParametersMap());
            }
            // schema rename
            IUpdateManager updateManager = process2.getUpdateManager();
            if (updateManager instanceof AbstractUpdateManager) {
                AbstractUpdateManager manager = (AbstractUpdateManager) updateManager;
                manager.setSchemaRenamedMap(getSchemaRenamedMap());
            }
            //
            for (EUpdateItemType type : types) {
                List<UpdateResult> updatesNeeded = updateManager.getUpdatesNeeded(type);
                if (updatesNeeded != null) {
                    resultList.addAll(updatesNeeded);
                }
                subMonitor.worked(1);
            }
        }
        subMonitor.done();
        return resultList;
    }

    private List<UpdateResult> getUpdatesNeededFromItems(IProgressMonitor parentMonitor, Item item,
            final Set<EUpdateItemType> types) {
        if (item == null || types == null) {
            return null;
        }
        IDesignerCoreService designerCoreService = CorePlugin.getDefault().getDesignerCoreService();
        if (designerCoreService == null) {
            return null;
        }
        //
        IProcess process = null;
        if (item instanceof ProcessItem) {
            process = designerCoreService.getProcessFromProcessItem((ProcessItem) item);
        } else if (item instanceof JobletProcessItem) {
            process = designerCoreService.getProcessFromJobletProcessItem((JobletProcessItem) item);
        }
        //
        if (process != null && process instanceof IProcess2) {
            IProcess2 process2 = (IProcess2) process;
            // for save item
            List<UpdateResult> resultFromProcess = getResultFromProcess(parentMonitor, process2, types);
            // set
            addItemForResult(process2, resultFromProcess);
            return resultFromProcess;

        }
        return null;
    }

    private void addItemForResult(IProcess2 process2, List<UpdateResult> updatesNeededFromItems) {
        if (process2 == null || updatesNeededFromItems == null) {
            return;
        }
        for (UpdateResult result : updatesNeededFromItems) {
            result.setItemProcess(process2);
        }
    }

    public static ERepositoryObjectType getTypeFromSource(String source) {
        if (source == null) {
            return null;
        }
        for (ERepositoryObjectType type : ERepositoryObjectType.values()) {
            String alias = type.getAlias();
            if (alias != null && source.startsWith(alias)) {
                return type;
            }
        }
        return null;
    }

    public static String getUpdateJobInfor(IProcess process) {
        if (process == null) {
            return UpdatesConstants.JOB;
        }
        StringBuffer infor = new StringBuffer();
        String prefix = UpdatesConstants.JOB;
        String label = null;
        String version = null;
        if (process instanceof IProcess2) {
            IProcess2 process2 = (IProcess2) process;
            if (process2.disableRunJobView()) { // for joblet
                prefix = UpdatesConstants.JOBLET;
            }
            label = process2.getProperty().getLabel();
            version = process2.getProperty().getVersion();
        }
        infor.append(prefix);
        if (label != null) {
            infor.append(UpdatesConstants.SPACE);
            infor.append(label);
            infor.append(UpdatesConstants.SPACE);
            infor.append(version);
        }
        return infor.toString();

    }

    /**
     * 
     * ggu Comment method "updateSchema".
     * 
     * for repository wizard.
     */
    public static boolean updateDBConnection(Connection connection) {
        return updateDBConnection(connection, true);
    }

    /**
     * 
     * ggu Comment method "updateQuery".
     * 
     * if show is false, will work for context menu action.
     */
    public static boolean updateDBConnection(Connection connection, boolean show) {
        RepositoryUpdateManager repositoryUpdateManager = new RepositoryUpdateManager(connection) {

            @Override
            public Set<EUpdateItemType> getTypes() {
                Set<EUpdateItemType> types = new HashSet<EUpdateItemType>();
                types.add(EUpdateItemType.NODE_PROPERTY);
                types.add(EUpdateItemType.JOB_PROPERTY_EXTRA);
                types.add(EUpdateItemType.JOB_PROPERTY_STATS_LOGS);
                return types;
            }

        };
        return repositoryUpdateManager.doWork(show);
    }

    /**
     * 
     * ggu Comment method "updateSchema".
     * 
     * for repository wizard.
     */
    public static boolean updateFileConnection(Connection connection) {
        return updateFileConnection(connection, true);
    }

    /**
     * 
     * ggu Comment method "updateQuery".
     * 
     * if show is false, will work for context menu action.
     */
    public static boolean updateFileConnection(Connection connection, boolean show) {
        RepositoryUpdateManager repositoryUpdateManager = new RepositoryUpdateManager(connection) {

            @Override
            public Set<EUpdateItemType> getTypes() {
                Set<EUpdateItemType> types = new HashSet<EUpdateItemType>();
                types.add(EUpdateItemType.NODE_PROPERTY);
                types.add(EUpdateItemType.NODE_SCHEMA);
                return types;
            }

        };
        return repositoryUpdateManager.doWork(show);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getTableIdAndNameMap(ConnectionItem connItem) {
        if (connItem == null) {
            return Collections.emptyMap();
        }
        Map<String, String> idAndNameMap = new HashMap<String, String>();
        EList tables = connItem.getConnection().getTables();
        if (tables != null) {
            for (MetadataTable table : (List<MetadataTable>) tables) {
                idAndNameMap.put(table.getId(), table.getLabel());
            }
        }
        return idAndNameMap;
    }

    public static Map<String, String> getOldTableIdAndNameMap(ConnectionItem connItem, MetadataTable metadataTable,
            boolean creation) {
        Map<String, String> oldTableMap = getTableIdAndNameMap(connItem);
        if (creation && metadataTable != null) {
            oldTableMap.remove(metadataTable.getId());
        }
        return oldTableMap;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getSchemaRenamedMap(ConnectionItem connItem, Map<String, String> oldTableMap) {
        if (connItem == null || oldTableMap == null) {
            return Collections.emptyMap();
        }

        Map<String, String> schemaRenamedMap = new HashMap<String, String>();

        final String prefix = connItem.getProperty().getId() + UpdatesConstants.SEGMENT_LINE;
        EList tables = connItem.getConnection().getTables();
        if (tables != null) {
            for (MetadataTable table : (List<MetadataTable>) tables) {
                String oldName = oldTableMap.get(table.getId());
                String newName = table.getLabel();
                if (oldName != null && !oldName.equals(newName)) {
                    schemaRenamedMap.put(prefix + oldName, prefix + newName);
                }
            }
        }
        return schemaRenamedMap;
    }

    /**
     * 
     * ggu Comment method "updateSchema".
     * 
     * for repository wizard.
     */
    public static boolean updateSingleSchema(ConnectionItem connItem, final MetadataTable newTable,
            final IMetadataTable oldMetadataTable, Map<String, String> oldTableMap) {
        if (connItem == null) {
            return false;
        }
        Map<String, String> schemaRenamedMap = RepositoryUpdateManager.getSchemaRenamedMap(connItem, oldTableMap);
        boolean update = !schemaRenamedMap.isEmpty();

        if (!update) {
            if (newTable != null && oldMetadataTable != null && oldTableMap.containsKey(newTable.getId())) {
                IMetadataTable newMetadataTable = ConvertionHelper.convert(newTable);
                update = !oldMetadataTable.sameMetadataAs(newMetadataTable, IMetadataColumn.OPTIONS_NONE);
            }
        }
        if (update) {
            // update
            return updateSchema(newTable, connItem, schemaRenamedMap, true);
        }
        return false;
    }

    public static boolean updateMultiSchema(ConnectionItem connItem, List<IMetadataTable> oldMetadataTable,
            Map<String, String> oldTableMap) {
        if (connItem == null) {
            return false;
        }
        Map<String, String> schemaRenamedMap = RepositoryUpdateManager.getSchemaRenamedMap(connItem, oldTableMap);
        boolean update = !schemaRenamedMap.isEmpty();

        if (!update) {
            if (oldMetadataTable != null) {
                List<IMetadataTable> newMetadataTable = RepositoryUpdateManager.getConversionMetadataTables(connItem
                        .getConnection());
                update = !RepositoryUpdateManager.sameAsMetadatTable(newMetadataTable, oldMetadataTable, oldTableMap);
            }
        }
        // update
        if (update) {
            return updateSchema(connItem, connItem, schemaRenamedMap, true);
        }
        return false;

    }

    private static boolean sameAsMetadatTable(List<IMetadataTable> newTables, List<IMetadataTable> oldTables,
            Map<String, String> oldTableMap) {
        if (newTables == null || oldTables == null) {
            return false;
        }

        Map<String, IMetadataTable> id2TableMap = new HashMap<String, IMetadataTable>();
        for (IMetadataTable oldTable : oldTables) {
            id2TableMap.put(oldTable.getId(), oldTable);
        }

        for (IMetadataTable newTable : newTables) {
            IMetadataTable oldTable = id2TableMap.get(newTable.getId());
            if (oldTableMap.containsKey(newTable.getId())) { // not a new created table.
                if (oldTable == null) {
                    return false;
                } else {
                    if (!newTable.sameMetadataAs(oldTable, IMetadataColumn.OPTIONS_NONE)) {
                        return false;
                    }
                }
            }
        }
        return true;

    }

    /**
     * 
     * ggu Comment method "updateSchema".
     * 
     * if show is false, will work for context menu action.
     */
    public static boolean updateSchema(final MetadataTable metadataTable, boolean show) {

        return updateSchema(metadataTable, null, null, show);
    }

    private static boolean updateSchema(final Object table, ConnectionItem connItem, Map<String, String> schemaRenamedMap,
            boolean show) {
        RepositoryUpdateManager repositoryUpdateManager = new RepositoryUpdateManager(table) {

            @Override
            public Set<EUpdateItemType> getTypes() {
                Set<EUpdateItemType> types = new HashSet<EUpdateItemType>();
                types.add(EUpdateItemType.NODE_SCHEMA);
                return types;
            }

        };

        // set renamed schema
        repositoryUpdateManager.setSchemaRenamedMap(schemaRenamedMap);

        return repositoryUpdateManager.doWork(show);
    }

    /**
     * 
     * ggu Comment method "updateQuery".
     * 
     * for repository wizard.
     */
    public static boolean updateQuery(QueriesConnection queryConn) {

        return updateQueryObject(queryConn, true);
    }

    /**
     * 
     * ggu Comment method "updateQuery".
     * 
     * if show is false, will work for context menu action.
     */
    public static boolean updateQuery(Query query, boolean show) {
        return updateQueryObject(query, show);
    }

    private static boolean updateQueryObject(Object parameter, boolean show) {
        RepositoryUpdateManager repositoryUpdateManager = new RepositoryUpdateManager(parameter) {

            @Override
            public Set<EUpdateItemType> getTypes() {
                Set<EUpdateItemType> types = new HashSet<EUpdateItemType>();
                types.add(EUpdateItemType.NODE_QUERY);
                return types;
            }

        };
        return repositoryUpdateManager.doWork(show);
    }

    /**
     * 
     * ggu Comment method "updateContext".
     * 
     * if show is false, will work for context menu action.
     */
    public static boolean updateContext(ContextItem item, boolean show) {
        return updateContext(null, item, show);
    }

    /**
     * 
     * ggu Comment method "updateContext".
     * 
     * for repository wizard.
     */
    public static boolean updateContext(JobContextManager repositoryContextManager, ContextItem item) {

        return updateContext(repositoryContextManager, item, true);
    }

    private static boolean updateContext(JobContextManager repositoryContextManager, ContextItem item, boolean show) {
        RepositoryUpdateManager repositoryUpdateManager = new RepositoryUpdateManager(item) {

            @Override
            public Set<EUpdateItemType> getTypes() {
                Set<EUpdateItemType> types = new HashSet<EUpdateItemType>();
                types.add(EUpdateItemType.CONTEXT);
                return types;
            }

        };
        if (repositoryContextManager != null) {
            Map<ContextItem, Map<String, String>> repositoryRenamedMap = new HashMap<ContextItem, Map<String, String>>();
            if (!repositoryContextManager.getNameMap().isEmpty()) {
                repositoryRenamedMap.put(item, repositoryContextManager.getNameMap());
            }
            repositoryUpdateManager.setContextRenamedMap(repositoryRenamedMap);

            // newly added parameters
            Map<ContextItem, Set<String>> newParametersMap = new HashMap<ContextItem, Set<String>>();
            if (!repositoryContextManager.getNewParameters().isEmpty()) {
                newParametersMap.put(item, repositoryContextManager.getNewParameters());
            }
            repositoryUpdateManager.setNewParametersMap(newParametersMap);

        }
        return repositoryUpdateManager.doWork(show);
    }

    public Map<ContextItem, Set<String>> getNewParametersMap() {
        return newParametersMap;
    }

    public void setNewParametersMap(Map<ContextItem, Set<String>> newParametersMap) {
        this.newParametersMap = newParametersMap;
    }

    public static boolean updateAllJob() {
        RepositoryUpdateManager repositoryUpdateManager = new RepositoryUpdateManager(null) {

            @Override
            public Set<EUpdateItemType> getTypes() {
                Set<EUpdateItemType> types = new HashSet<EUpdateItemType>();
                for (EUpdateItemType type : EUpdateItemType.values()) {
                    types.add(type);
                }
                return types;
            }

        };
        return repositoryUpdateManager.doWork();
    }

    @SuppressWarnings("unchecked")
    public static List<IMetadataTable> getConversionMetadataTables(Connection conn) {
        if (conn == null) {
            return Collections.emptyList();
        }
        List<IMetadataTable> tables = new ArrayList<IMetadataTable>();

        EList tables2 = conn.getTables();
        if (tables2 != null) {
            for (org.talend.core.model.metadata.builder.connection.MetadataTable originalTable : (List<org.talend.core.model.metadata.builder.connection.MetadataTable>) tables2) {
                IMetadataTable conversionTable = ConvertionHelper.convert(originalTable);
                tables.add(conversionTable);
            }
        }

        return tables;
    }

}
