package org.metro.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.metro.dao.mapper.ObjectSlotMapper;
import org.metro.dao.model.ObjectSlot;
import org.metro.web.vo.RestErrors;
import org.metro.web.vo.RestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ObjectStorageService {

    private static final Pattern directory = Pattern.compile("^[^<>:;,?\"*|/]+$");
    private static final Pattern extension = Pattern.compile(".([0-9a-zA-Z_]+)$");

    private static ObjectSlot buildObjSlot(MultipartFile file) {
        String filename = Validate.notBlank(file.getOriginalFilename());
        String contentType = Validate.notBlank(file.getContentType());

        MimeType mimeType = MimeType.valueOf(contentType);
        Matcher matchDir = directory.matcher(mimeType.getType());
        if (! matchDir.find()) {
            throw new RestException(RestErrors.UnsupportedMediaType);
        }

        String fileExt = "";
        Matcher matchExt = extension.matcher(filename);
        if (matchExt.find()) {
            fileExt = "/" + matchExt.group(1);
        }

        ObjectSlot os = new ObjectSlot();
        os.setCreateDate(new Date());
        os.setUpdateDate(new Date());
        os.setObjMime(contentType + fileExt);
        os.setObjName(file.getName());
        return os;
    }

    @Value("oss.storage-path")
    private String storagePath;

    @Autowired
    private ObjectSlotMapper objectSlotMapper;

    @PostConstruct
    public void prepare() throws IOException {
        Files.createDirectories(Paths.get(storagePath));
    }

    @Transactional
    public void storeObject(MultipartFile file) {
        ObjectSlot os = buildObjSlot(file);
        objectSlotMapper.insertSelective(os);
        try {
            file.transferTo(Paths.get(os.getPath()));
        } catch (IOException e) {
            log.error("保存文件失败", e);
            throw new RestException(RestErrors.UnknownError);
        }
    }

    @Transactional
    public void refreshObject(Integer objId, MultipartFile file) {
        ObjectSlot oldSlot = objectSlotMapper.selectByPrimaryKey(objId);
        if (oldSlot == null) {
            throw new RestException(RestErrors.NotFound);
        }

        ObjectSlot newSlot = buildObjSlot(file);
        newSlot.setObjId(oldSlot.getObjId());
        objectSlotMapper.updateByPrimaryKeySelective(newSlot);
        try {
            String oldFile = oldSlot.getFileName();
            String newFile = newSlot.getFileName();
            if (newFile.equals(oldFile)) {
                Files.copy(file.getInputStream(), Paths.get(oldFile), StandardCopyOption.REPLACE_EXISTING);
            } else {
                file.transferTo(Paths.get(newFile));
                Files.deleteIfExists(Paths.get(oldFile));
            }
        } catch (IOException e) {
            log.error("保存文件失败", e);
            throw new RestException(RestErrors.UnknownError);
        }
    }

    public void loadObject(Integer objId, HttpServletResponse response) {
        ObjectSlot objectSlot = objectSlotMapper.selectByPrimaryKey(objId);
        if (objectSlot == null) {
            throw new RestException(HttpStatus.NOT_FOUND, RestErrors.NotFound);
        }

        String contentType = objectSlot.getContentType();
        response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        response.setContentType(contentType);
        try {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + URLEncoder.encode(objectSlot.getFileName(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {}

        try {
            InputStream inputStream = Files.newInputStream(Paths.get(objectSlot.getPath()));
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        } catch (IOException e) {
            log.error("读取文件失败", e);
            if (e instanceof FileNotFoundException) {
                throw new RestException(HttpStatus.NOT_FOUND, RestErrors.NotFound);
            }
            throw new RestException(HttpStatus.INTERNAL_SERVER_ERROR, RestErrors.UnknownError);
        }
    }

    public List<ObjectSlot> listImage(Integer previousId, String matchName, int limit) {
        ObjectSlot filter = new ObjectSlot();
        filter.setObjId(previousId);
        filter.setObjMime("image/%");
        filter.setObjName(matchName);
        return objectSlotMapper.listByType(filter, limit);
    }

}
