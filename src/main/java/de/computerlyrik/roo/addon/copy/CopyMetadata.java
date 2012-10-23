package de.computerlyrik.roo.addon.copy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * This type produces metadata for a new ITD. It uses an {@link ItdTypeDetailsBuilder} provided by 
 * {@link AbstractItdTypeDetailsProvidingMetadataItem} to register a field in the ITD and a new method.
 * 
 * @since 1.1.0
 */
public class CopyMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	

    // Constants
    private static final String PROVIDES_TYPE_STRING = CopyMetadata.class.getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

    private final List<FieldMetadata> locatedFields;

    public static final String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }
    
    public static final String createIdentifier(JavaType javaType, LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
    }

    public static final JavaType getJavaType(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static final LogicalPath getPath(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static boolean isValid(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }
    
    public CopyMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata,  final List<FieldMetadata> locatedFields) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
                
        this.locatedFields = locatedFields;
        Validate.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
            
        //test if locatedFields are empty
        if (!CollectionUtils.isEmpty(locatedFields)) {
            builder.addMethod(getCopyMethod());
        }
        
        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }
    
    private MethodMetadata getCopyMethod() {
        // Specify the desired method name
        JavaSymbolName methodName = new JavaSymbolName("copy");
        
        // Check if a method with the same signature already exists in the target type
        final MethodMetadata method = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
        if (method != null) {
            // If it already exists, just return the method and omit its generation via the ITD
            return method;
        }
        
        // Define method annotations (none in this case)
        List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        
        // Define method throws types (none in this case)
        List<JavaType> throwsTypes = new ArrayList<JavaType>();
        
        // Define method parameter types (none in this case)
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        //parameterTypes.add(new AnnotatedJavaType(
        //		governorTypeDetails.getType()));
        // Define method parameter names (none in this case)
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        //parameterNames.add(new JavaSymbolName(
        //		governorTypeDetails.getName().getSimpleTypeName().toLowerCase()));
        
        // Create the method body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
    	bodyBuilder.appendFormalLine(governorTypeDetails.getName().getSimpleTypeName()+" p = new " +
    			governorTypeDetails.getName().getSimpleTypeName()+"();");

        for (FieldMetadata field : locatedFields)
        {
        	String fieldName = field.getFieldName().getSymbolName();
        	String getter = "get"+field.getFieldName().getSymbolNameCapitalisedFirstLetter();
        	String setter = "set"+field.getFieldName().getSymbolNameCapitalisedFirstLetter();
        	JavaType fieldType = field.getFieldType();
        	//ClassOrInterfaceTypeDetails citd;
        	if (fieldType.isCommonCollectionType())
        	{
        		JavaType collectionType = fieldType.getParameters().get(0);
        		System.out.println("got List with generic type:"+collectionType.toString());
        		//citd = typeLocationService.getTypeDetails(fieldType);
        		
    			bodyBuilder.appendFormalLine("for ( "+collectionType+" t : "+fieldName+" ) {");

        	    if (collectionType.isPrimitive())
        			bodyBuilder.appendFormalLine("p."+getter+".add("+field.getFieldName()+")");
        	    else if(MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType(RooCopy.class.getName())) != null)
        			bodyBuilder.appendFormalLine("p."+getter+".add("+field.getFieldName()+".copy())");
        	    else
        			bodyBuilder.appendFormalLine("p."+getter+".add("+field.getFieldName()+")");
    			
        	    bodyBuilder.appendFormalLine("}");

        	}
        	else if (fieldType.isPrimitive()) {
    	    	bodyBuilder.appendFormalLine("p."+setter+"(this."+getter+"());");
        	}
        	//TODO:
        	//This would happen, if class is some reference to another complex class
        	//If this class contains a copy method, it will be used
        	//Otherwise, only reference will be set! Use excludeFields, if you do not want them in your copy!
        	else {
        		//citd = typeLocationService.getTypeDetails(fieldType);
        	    if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType(RooCopy.class.getName())) != null)
    	    		bodyBuilder.appendFormalLine("p."+setter+"(this."+getter+"().copy());");
        	    else 
        	    	bodyBuilder.appendFormalLine("p."+setter+"(this."+getter+"());");

        	}
        }
    	bodyBuilder.appendFormalLine("return p;");

        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, governorTypeDetails.getType() , parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        methodBuilder.setThrowsTypes(throwsTypes);
        
        return methodBuilder.build(); // Build and return a MethodMetadata instance
    }
        
    private MethodMetadata methodExists(JavaSymbolName methodName, List<AnnotatedJavaType> paramTypes) {
        // We have no access to method parameter information, so we scan by name alone and treat any match as authoritative
        // We do not scan the superclass, as the caller is expected to know we'll only scan the current class
        for (MethodMetadata method : governorTypeDetails.getDeclaredMethods()) {
            if (method.getMethodName().equals(methodName) && method.getParameterTypes().equals(paramTypes)) {
                // Found a method of the expected name; we won't check method parameters though
                return method;
            }
        }
        return null;
    }
    
    // Typically, no changes are required beyond this point
    
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}
