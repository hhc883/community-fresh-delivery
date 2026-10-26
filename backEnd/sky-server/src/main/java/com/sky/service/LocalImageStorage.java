package com.sky.service;

import com.sky.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/** Local product images work without an external object-storage account. */
@Service
public class LocalImageStorage {
    private final Path directory;

    public LocalImageStorage(@Value("${app.upload-dir:./.local/uploads}") String directory) {
        this.directory = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 2 * 1024 * 1024) {
            throw new BaseException("请选择不超过2MB的PNG或JPEG图片");
        }
        try {
            byte[] bytes = file.getBytes();
            String format;
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new BaseException("图片格式无效，仅支持PNG或JPEG");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!(format.equals("png") || format.equals("jpeg"))) {
                        throw new BaseException("仅支持PNG或JPEG图片");
                    }
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 8000 || height > 8000 || (long) width * height > 20000000) {
                        throw new BaseException("图片尺寸过大，请缩小后上传");
                    }
                    if (reader.read(0) == null) throw new BaseException("图片内容损坏");
                } finally { reader.dispose(); }
            }
            String filename = UUID.randomUUID() + (format.equals("png") ? ".png" : ".jpg");
            Files.createDirectories(directory);
            Files.write(directory.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
            return "/uploads/" + filename;
        } catch (IOException error) {
            throw new BaseException("图片上传失败，请检查图片内容或本地存储目录");
        }
    }

    public Path find(String filename) {
        if (!filename.matches("[a-f0-9-]{36}\\.(png|jpg)")) return null;
        Path target = directory.resolve(filename).normalize();
        return target.startsWith(directory) && Files.isRegularFile(target) ? target : null;
    }
}
