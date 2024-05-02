package com.ekenya.chamakyc.service.impl;

import com.ekenya.chamakyc.service.Interfaces.FileHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class FileHandlerServiceTests {
    @Autowired
    private FileHandlerService fileService;

//    @Test
//    public void returnURIForUpload(){
//        FilePart filePart = mock(FilePart.class);
//        given(filePart.filename()).willReturn("TestImage.png");
//        String uri= fileService.uploadFile(filePart);
//        log.info(uri);
//        UrlValidator urlValidator=new UrlValidator();
//        Assertions.assertThat(urlValidator.isValid(uri)).isTrue();
//    }

//    @Test
//    public void loadFileByName(){
//        Resource resource= fileService.load("Schools.PNG");
//        Assertions.assertThat(resource).isNotEqualTo(null);
//    }


}
