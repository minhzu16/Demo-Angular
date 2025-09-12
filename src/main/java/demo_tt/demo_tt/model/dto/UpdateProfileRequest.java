package demo_tt.demo_tt.model.dto;

import demo_tt.demo_tt.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Tên đầy đủ không được để trống")
    @Size(max = 100, message = "Tên đầy đủ không được quá 100 ký tự")
    private String fullName;
    
    private Integer age;
    
    @Size(max = 15, message = "Số điện thoại không được quá 15 ký tự")
    private String phoneNumber;
    
    @Size(max = 255, message = "Địa chỉ không được quá 255 ký tự")
    private String address;
    
    private UserEntity.Gender gender;
    
    @Size(max = 100, message = "Nơi làm việc không được quá 100 ký tự")
    private String workplace;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
