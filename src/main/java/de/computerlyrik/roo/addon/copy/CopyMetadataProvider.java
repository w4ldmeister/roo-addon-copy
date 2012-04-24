package de.computerlyrik.roo.addon.copy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Provides {@link CopyMetadata}. This type is called by Roo to retrieve the metadata for this add-on.
 * Use this type to reference external types and services needed by the metadata type. Register metadata triggers and
 * dependencies here. Also define the unique add-on ITD identifier.
 * 
 * @since 1.1
 */
@Component
@Service
public final class CopyMetadataProvider extends AbstractItdMetadataProvider {

    /**
     * The activate method for this OSGi component, this will be called by the OSGi container upon bundle activation 
     * (result of the 'addon install' command) 
     * 
     * @param context the component context can be used to get access to the OSGi container (ie find out if certain bundles are active)
     */
    protected void activate(ComponentContext context) {
        metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
        addMetadataTrigger(new JavaType(RooCopy.class.getName()));
    }
    
    /**
     * The deactivate method for this OSGi component, this will be called by the OSGi container upon bundle deactivation 
     * (result of the 'addon uninstall' command) 
     * 
     * @param context the component context can be used to get access to the OSGi container (ie find out if certain bundles are active)
     */
    protected void deactivate(ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
        removeMetadataTrigger(new JavaType(RooCopy.class.getName()));    
    }
    
    /**
     * Return an instance of the Metadata offered by this add-on
     */
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
        // Pass dependencies required by the metadata in through its constructor
    	
        final JavaType javaType = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails().getName();
    	
        final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
        if (memberDetails == null) {
            return null;
        }
        
        final List<FieldMetadata> locatedFields = locateFields(javaType,
                new String[0], memberDetails,
                metadataIdentificationString);
        return new CopyMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, locatedFields);
    }
    
    /**
     * Define the unique ITD file name extension, here the resulting file name will be **_ROO_Copy.aj
     */
    public String getItdUniquenessFilenameSuffix() {
        return "Copy";
    }

    protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
        JavaType javaType = CopyMetadata.getJavaType(metadataIdentificationString);
        LogicalPath path = CopyMetadata.getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }
    
    protected String createLocalIdentifier(JavaType javaType, LogicalPath path) {
        return CopyMetadata.createIdentifier(javaType, path);
    }

    public String getProvidesType() {
        return CopyMetadata.getMetadataIdentiferType();
    }
    
	private List<FieldMetadata> locateFields(final JavaType javaType,
            final String[] excludeFields, final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        final SortedSet<FieldMetadata> locatedFields = new TreeSet<FieldMetadata>(
                new Comparator<FieldMetadata>() {
                    public int compare(final FieldMetadata l,
                            final FieldMetadata r) {
                        return l.getFieldName().compareTo(r.getFieldName());
                    }
                });

        final List<?> excludeFieldsList = CollectionUtils
                .arrayToList(excludeFields);
        final FieldMetadata versionField = persistenceMemberLocator
                .getVersionField(javaType);

        for (final FieldMetadata field : memberDetails.getFields()) {
            if (excludeFieldsList
                    .contains(field.getFieldName().getSymbolName())) {
                continue;
            }
            if (Modifier.isStatic(field.getModifier())
                    || Modifier.isTransient(field.getModifier())
                    || field.getFieldType().isCommonCollectionType()
                    || field.getFieldType().isArray()) {
                continue;
            }
            if (versionField != null
                    && field.getFieldName().equals(versionField.getFieldName())) {
                continue;
            }

            locatedFields.add(field);
            metadataDependencyRegistry.registerDependency(
                    field.getDeclaredByMetadataId(),
                    metadataIdentificationString);
        }

        return new ArrayList<FieldMetadata>(locatedFields);
    }
}