package demo_tt.demo_tt.converter;

import demo_tt.demo_tt.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    private static ApplicationContext applicationContext;
    
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        EncryptedStringConverter.applicationContext = applicationContext;
    }
    
    private EncryptionService getEncryptionService() {
        if (applicationContext != null) {
            return applicationContext.getBean(EncryptionService.class);
        }
        return null;
    }
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        
        EncryptionService encryptionService = getEncryptionService();
        if (encryptionService == null) {
            // Fallback nếu service chưa được inject
            System.err.println("EncryptionService not available, storing plain text");
            return attribute;
        }
        
        try {
            String encrypted = encryptionService.encrypt(attribute);
            System.out.println("Encrypted: " + attribute + " -> " + encrypted.substring(0, Math.min(20, encrypted.length())) + "...");
            return encrypted;
        } catch (Exception e) {
            // Log error và trả về giá trị gốc nếu mã hóa thất bại
            System.err.println("Error encrypting data: " + e.getMessage());
            return attribute;
        }
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        
        EncryptionService encryptionService = getEncryptionService();
        if (encryptionService == null) {
            // Fallback nếu service chưa được inject
            System.err.println("EncryptionService not available, returning plain text");
            return dbData;
        }
        
        try {
            String decrypted = encryptionService.decrypt(dbData);
            System.out.println("Decrypted: " + dbData.substring(0, Math.min(20, dbData.length())) + "... -> " + decrypted);
            return decrypted;
        } catch (Exception e) {
            // Log error và trả về giá trị gốc nếu giải mã thất bại
            System.err.println("Error decrypting data: " + e.getMessage());
            return dbData;
        }
    }
}
