/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rabobank.engine.statement.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabobank.engine.statement.config.Constants;
import com.rabobank.engine.statement.config.Utility;
import com.rabobank.engine.statement.exception.ClientException;
import com.rabobank.engine.statement.exception.ServerException;
import com.rabobank.engine.statement.manager.RaboCustomerStatementManager;

/**
 * This controller class is process the customer statements and provide the
 * error report.
 *
 * @author KARTHIK
 */

@Path("/rabo/statement")
public class RaboCustomerStatementController {

	private static final Logger logger = LoggerFactory.getLogger(RaboCustomerStatementController.class);

	@Inject
	private RaboCustomerStatementManager raboCustomStatementManager;

	@Inject
	private Utility utility;

	/**
	 *
	 * This controller method is to perform process the customer statements and
	 * provide the error report.
	 *
	 * @param response
	 * @param request
	 * @param file
	 * @throws IOException
	 */
	@POST
	@Path(value = "/report")
	@Consumes("multipart/form-data")
	public Response processStatement(MultipartFormDataInput input) throws IOException {
		logger.info("Start the statement process");

		String fileType = null;
		Map<String, String> map = null;
		InputStream istream = null;
		try {
			Map<String, List<InputPart>> formParts = input.getFormDataMap();
			List<InputPart> inPart = formParts.get("file");
			for (InputPart inputPart : inPart) {
				MultivaluedMap<String, String> headers = inputPart.getHeaders();
				fileType = getFileType(parseFileName(headers));
				istream = inputPart.getBody(InputStream.class, null);
			}
			if (Constants.XML_FILETYPE.equalsIgnoreCase(fileType)) {
				map = raboCustomStatementManager.processStatement(utility.getXMLRecords(istream));
			} else if (Constants.CSV_FILETYPE.equalsIgnoreCase(fileType)) {
				map = raboCustomStatementManager.processStatement(utility.getCSVRecords(istream));
			} else {
				throw new ClientException("RABO1011");
			}
			logger.info("End of the statement process");
			return writeMessage(map);
		} catch (ClientException ex) {
			logger.error(ex.getMessage(), ex);
			return setClientException(ex);
		} catch (ServerException ex) {
			logger.error(ex.getMessage(), ex);
			return setServerException(ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return setServerException();
		}
	}

	/**
	 *
	 * This method is to perform set server exception
	 *
	 * @param response
	 * @param printWriter
	 * @param ex
	 */
	private Response setServerException(ServerException ex) {
		logger.info("Set server exception");
		if (utility.isNullOrEmpty(ex.getCode())) {
			return setServerException();
		}
		return utility.getErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.getCode(), ex.getMessage());

	}

	/**
	 *
	 * This method is to perform set client exception
	 *
	 * @param response
	 * @param printWriter
	 * @param ex
	 */
	private Response setClientException(ClientException ex) {
		logger.info("Set client exception");
		if (utility.isNullOrEmpty(ex.getCode())) {
			return setServerException();
		}
		return utility.getErrorResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage());

	}

	/**
	 *
	 * This method is to perform set server exception
	 *
	 * @param response
	 * @param printWriter
	 */
	private Response setServerException() {
		logger.info("Set server exception");
		ServerException serverException = new ServerException("RABO1012");
		return utility.getErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, serverException.getCode(),
				serverException.getMessage());
	}

	/**
	 *
	 * This method is to perform get file type
	 *
	 * @param file
	 */
	private String getFileType(String fileName) {
		logger.info("Get file type");
		return fileName.substring(fileName.lastIndexOf('.'), fileName.length());
	}

	/**
	 *
	 * This method is to perform write success message on the response.
	 *
	 * @param map
	 * @param response
	 */
	public Response writeMessage(Map<String, String> map) {
		logger.info("Write message");
		StringBuilder builder = new StringBuilder();
		builder.append("<HTML><HEAD><TITLE>RaboBank Customer Statement Processor</TITLE></HEAD><BODY>");
		builder.append("<DIV align=\"center\">");
		builder.append("<TABLE border=\"1\" summary=\"Report\" frame=\"box\">");
		builder.append("<CAPTION><EM>Report</EM></CAPTION>");
		builder.append("<TR><TH>Reference</TH><TH>Description</TH></TR>");
		for (String key : map.keySet()) {
			builder.append("<TR><TD>").append(key.substring(0, key.indexOf(Constants.UNDER_SCORE))).append("</TD>")
					.append("<TD>").append(map.get(key)).append("</TD></TR>");
		}
		builder.append("</TABLE></DIV>");
		builder.append("</BODY></HTML>");
		String message = builder.toString();
		logger.info(message);
		return utility.getResponseBuilder(HttpStatus.SC_OK).entity(message).build();
	}

	private String parseFileName(MultivaluedMap<String, String> headers) {

		String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
		for (String name : contentDispositionHeader) {
			if ((name.trim().startsWith("filename"))) {
				String[] tmp = name.split("=");
				return tmp[1].trim().replaceAll("\"", "");
			}
		}
		return "randomName.csv";
	}
}
