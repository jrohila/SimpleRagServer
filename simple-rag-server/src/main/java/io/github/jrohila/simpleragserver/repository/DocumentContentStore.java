/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.entity.DocumentEntity;
import org.springframework.content.commons.store.ContentStore;

/**
 *
 * @author Lenovo
 */
public interface DocumentContentStore extends ContentStore<DocumentEntity, String> {
}
