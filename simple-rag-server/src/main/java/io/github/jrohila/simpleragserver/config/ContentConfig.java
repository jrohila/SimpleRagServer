/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.content.fs.config.EnableFilesystemStores;

@Configuration
@EnableFilesystemStores
class ContentConfig {

    /**
  @Bean(name = "filesystemRoot")
  File filesystemRoot(@Value("${spring.content.fs.filesystemRoot}") String root) {
    File dir = new File(root);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IllegalStateException("Cannot create FS root: " + dir.getAbsolutePath());
    }
    return dir;
  }

  @Bean
  org.springframework.content.fs.io.FileSystemResourceLoader fileSystemResourceLoader(
      @Qualifier("filesystemRoot") File root) {
    return new org.springframework.content.fs.io.FileSystemResourceLoader(root.getAbsolutePath());
  }**/
}

