/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package orgomg.cwm.analysis.datamining.provider;


import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ViewerNotification;

import orgomg.cwm.analysis.datamining.DataminingPackage;
import orgomg.cwm.analysis.datamining.SupervisedMiningSettings;

import orgomg.cwm.objectmodel.core.CorePackage;

import orgomg.cwm.objectmodel.core.provider.Cwm_mipEditPlugin;

/**
 * This is the item provider adapter for a {@link orgomg.cwm.analysis.datamining.SupervisedMiningSettings} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class SupervisedMiningSettingsItemProvider
    extends MiningSettingsItemProvider
    implements	
        IEditingDomainItemProvider,	
        IStructuredItemContentProvider,	
        ITreeItemContentProvider,	
        IItemLabelProvider,	
        IItemPropertySource {
    /**
     * This constructs an instance from a factory and a notifier.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public SupervisedMiningSettingsItemProvider(AdapterFactory adapterFactory) {
        super(adapterFactory);
    }

    /**
     * This returns the property descriptors for the adapted class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
        if (itemPropertyDescriptors == null) {
            super.getPropertyDescriptors(object);

            addConfidenceAttributeNamePropertyDescriptor(object);
            addPredictedAttributeNamePropertyDescriptor(object);
            addCostFunctionPropertyDescriptor(object);
        }
        return itemPropertyDescriptors;
    }

    /**
     * This adds a property descriptor for the Confidence Attribute Name feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected void addConfidenceAttributeNamePropertyDescriptor(Object object) {
        itemPropertyDescriptors.add
            (createItemPropertyDescriptor
                (((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
                 getResourceLocator(),
                 getString("_UI_SupervisedMiningSettings_confidenceAttributeName_feature"),
                 getString("_UI_PropertyDescriptor_description", "_UI_SupervisedMiningSettings_confidenceAttributeName_feature", "_UI_SupervisedMiningSettings_type"),
                 DataminingPackage.Literals.SUPERVISED_MINING_SETTINGS__CONFIDENCE_ATTRIBUTE_NAME,
                 true,
                 false,
                 false,
                 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
                 null,
                 null));
    }

    /**
     * This adds a property descriptor for the Predicted Attribute Name feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected void addPredictedAttributeNamePropertyDescriptor(Object object) {
        itemPropertyDescriptors.add
            (createItemPropertyDescriptor
                (((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
                 getResourceLocator(),
                 getString("_UI_SupervisedMiningSettings_predictedAttributeName_feature"),
                 getString("_UI_PropertyDescriptor_description", "_UI_SupervisedMiningSettings_predictedAttributeName_feature", "_UI_SupervisedMiningSettings_type"),
                 DataminingPackage.Literals.SUPERVISED_MINING_SETTINGS__PREDICTED_ATTRIBUTE_NAME,
                 true,
                 false,
                 false,
                 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
                 null,
                 null));
    }

    /**
     * This adds a property descriptor for the Cost Function feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected void addCostFunctionPropertyDescriptor(Object object) {
        itemPropertyDescriptors.add
            (createItemPropertyDescriptor
                (((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
                 getResourceLocator(),
                 getString("_UI_SupervisedMiningSettings_costFunction_feature"),
                 getString("_UI_PropertyDescriptor_description", "_UI_SupervisedMiningSettings_costFunction_feature", "_UI_SupervisedMiningSettings_type"),
                 DataminingPackage.Literals.SUPERVISED_MINING_SETTINGS__COST_FUNCTION,
                 true,
                 false,
                 false,
                 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
                 null,
                 null));
    }

    /**
     * This returns SupervisedMiningSettings.gif.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public Object getImage(Object object) {
        return overlayImage(object, getResourceLocator().getImage("full/obj16/SupervisedMiningSettings"));
    }

    /**
     * This returns the label text for the adapted class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public String getText(Object object) {
        String label = ((SupervisedMiningSettings)object).getName();
        return label == null || label.length() == 0 ?
            getString("_UI_SupervisedMiningSettings_type") :
            getString("_UI_SupervisedMiningSettings_type") + " " + label;
    }

    /**
     * This handles model notifications by calling {@link #updateChildren} to update any cached
     * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public void notifyChanged(Notification notification) {
        updateChildren(notification);

        switch (notification.getFeatureID(SupervisedMiningSettings.class)) {
            case DataminingPackage.SUPERVISED_MINING_SETTINGS__CONFIDENCE_ATTRIBUTE_NAME:
            case DataminingPackage.SUPERVISED_MINING_SETTINGS__PREDICTED_ATTRIBUTE_NAME:
            case DataminingPackage.SUPERVISED_MINING_SETTINGS__COST_FUNCTION:
                fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
                return;
        }
        super.notifyChanged(notification);
    }

    /**
     * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s describing the children
     * that can be created under this object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object) {
        super.collectNewChildDescriptors(newChildDescriptors, object);
    }

    /**
     * This returns the label text for {@link org.eclipse.emf.edit.command.CreateChildCommand}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public String getCreateChildText(Object owner, Object feature, Object child, Collection<?> selection) {
        Object childFeature = feature;
        Object childObject = child;

        boolean qualify =
            childFeature == CorePackage.Literals.NAMESPACE__OWNED_ELEMENT ||
            childFeature == DataminingPackage.Literals.MINING_SETTINGS__ATTRIBUTE_USAGE ||
            childFeature == CorePackage.Literals.CLASSIFIER__FEATURE;

        if (qualify) {
            return getString
                ("_UI_CreateChild_text2",
                 new Object[] { getTypeText(childObject), getFeatureText(childFeature), getTypeText(owner) });
        }
        return super.getCreateChildText(owner, feature, child, selection);
    }

    /**
     * Return the resource locator for this item provider's resources.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public ResourceLocator getResourceLocator() {
        return Cwm_mipEditPlugin.INSTANCE;
    }

}
