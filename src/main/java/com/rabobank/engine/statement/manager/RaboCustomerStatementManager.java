/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rabobank.engine.statement.manager;

import java.util.Map;

import com.rabobank.engine.statement.exception.ClientException;
import com.rabobank.engine.statement.model.Records;

/**
 *
 * @author KARTHIK
 */
public interface RaboCustomerStatementManager {
    
	Map<String, String> processStatement(Records records) throws ClientException;
    
}
