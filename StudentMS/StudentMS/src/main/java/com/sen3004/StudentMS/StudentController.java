package com.sen3004.StudentMS;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/students")
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public String getStudents(Model model) {
        List<Student> students = studentRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("students", students);
        return "students/index";
    }

    @GetMapping("/add")
    public String addStudentPage(Model model) {
        StudentDto studentDto = new StudentDto();
        model.addAttribute("studentDto", studentDto);
        return "students/addStudentPage";
    }

    @PostMapping("/add")
    public String addStudent(@Valid @ModelAttribute StudentDto studentDto, BindingResult result) {
        if (result.hasErrors()) {
            return "students/addStudentPage";
        }

        MultipartFile image = studentDto.getImage();
        if (image.isEmpty()) {
            result.addError(new FieldError("studentDto", "image", "Image is mandatory"));
            return "students/addStudentPage";
        }

        String filename = saveImage(image);
        if (filename == null) {
            result.addError(new FieldError("studentDto", "image", "Failed to save the image"));
            return "students/addStudentPage";
        }

        Student student = new Student();
        student.setName(studentDto.getName());
        student.setEmail(studentDto.getEmail());
        student.setDepartment(studentDto.getDepartment());
        student.setText(studentDto.getText());
        student.setCreatedAt(new Date());
        student.setImage(filename);
        studentRepository.save(student);

        return "redirect:/students";
    }

    private String saveImage(MultipartFile image) {
        if (!image.getContentType().equals("image/jpeg") && !image.getContentType().equals("image/png")) {
            return null;
        }

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = new Date().getTime() + "_" + image.getOriginalFilename();
            image.transferTo(uploadPath.resolve(filename));
            return filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @GetMapping("/edit")
    public String editStudentPage(Model model, @RequestParam int id) {
        Student student = studentRepository.findById(id).get();
        model.addAttribute("student", student);

        StudentDto studentDto = new StudentDto();
        studentDto.setName(student.getName());
        studentDto.setEmail(student.getEmail());
        studentDto.setDepartment(student.getDepartment());
        studentDto.setText(student.getText());
        model.addAttribute("studentDto", studentDto);
        return "students/editStudentPage";
    }

    @PostMapping("/edit")
    public String editStudent(@Valid @ModelAttribute StudentDto studentDto, BindingResult result, @RequestParam int id, Model model) {

        Student student = studentRepository.findById(id).get();
        model.addAttribute("student", student);

        if (result.hasErrors()) {
            return "students/editStudentPage";
        }

        String upload = "public/images/";
        Path old = Paths.get(upload + student.getImage());

        try {
            Files.delete(old);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MultipartFile image = studentDto.getImage();
        Date createdAt = new Date();
        String temp = createdAt.getTime() + " " + image.getOriginalFilename();

        try {
            Files.copy(image.getInputStream(), Paths.get(upload + temp));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        student.setImage(temp);

        student.setName(studentDto.getName());
        student.setEmail(studentDto.getEmail());
        student.setDepartment(studentDto.getDepartment());
        student.setText(studentDto.getText());
        studentRepository.save(student);
        return "redirect:/students";
    }

    @GetMapping("/delete")
    public String deleteStudent(@RequestParam int id) {
        Student student = studentRepository.findById(id).get();
        String upload = "public/images/";
        Path old = Paths.get(upload + student.getImage());

        try {
            Files.delete(old);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        studentRepository.deleteById(id);
        return "redirect:/students";
    }
}
