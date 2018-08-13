/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ibm.atlas.model.files.CreateFileRequest;
import com.ibm.atlas.model.files.GetFileAttributesResponse;
import com.ibm.atlas.model.files.GetFileChildAttributesResponse;
import com.ibm.atlas.model.files.GetFileContentResponse;
import com.ibm.atlas.model.files.UpdateFileContentsRequest;
import com.ibm.atlas.model.files.UssFileType;
import com.ibm.atlas.model.files.ZosmfPostFileRequest;
import com.ibm.atlas.utilities.Utils;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.exceptions.AtlasException;
import com.ibm.atlas.webservice.exceptions.BadRequestException;
import com.ibm.atlas.webservice.exceptions.ForbiddenException;
import com.ibm.atlas.webservice.exceptions.NotFoundException;
import com.ibm.json.java.JSONObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FilePermissionsHelper.class, ZosmfFilesService.class })
public class ZosmfFilesServiceTest extends AbstractZosmfServiceTest<ZosmfService> {
	
	private static final String DUMMY_PATH = "/u/dummy/path";
	private static final String DUMMY_PATH2 = "u/dummy/path2";
	static final String CONTEXT_ROOT = "zosmf"; //$NON-NLS-1$
	
	private String path;

	ZosmfFilesService filesService;

	@Before
	public void setUp() throws Exception {
		filesService = new ZosmfFilesService();
		filesService.log = mock(Logger.class);
		filesService.uriInfo = Mockito.mock(UriInfo.class);
		super.init(filesService);
		path = DUMMY_PATH + "/" + Utils.generateRandomString();
	}

	@Test
	public void testCreateFileOrDirectoryWorksForValidFile() throws Exception {
		testValidCreate(UssFileType.file);
	}

	@Test
	public void testCreateFileOrDirectoryWorksForValidFileWithPermissions() throws Exception {
		String pathWithoutLeadingSlash = DUMMY_PATH2 + "file";
		testValidCreate(UssFileType.file, pathWithoutLeadingSlash, "740", "rwxr-----");
	}

	@Test
	public void testCreateFileOrDirectoryWorksForValidDirectory() throws Exception {
		testValidCreate(UssFileType.directory);
	}

	@Test
	public void testCreateFileOrDirectoryWorksForValidDirectoryWithPermissions() throws Exception {
		testValidCreate(UssFileType.file, path, "555", "r-xr-xr-x");
	}

	@Test
	public void exceptionInFilePermissionsHelperShouldBeCascadedUp() throws Exception {
		mockStatic(FilePermissionsHelper.class);
		BadRequestException expected = new BadRequestException("DUMMY");
		when(FilePermissionsHelper.convertPermissionsNumericToSymbolicForm("999")).thenThrow(expected);

		CreateFileRequest request = CreateFileRequest.builder().permissions("999").build();

		shouldThrow(expected, () -> filesService.createFileOrDirectory(request));
	}

	@Test
	public void creatingExistingFileShouldThrowException() throws Exception {
		BadRequestException expected = new BadRequestException(String.format(Messages.getString("ZosmfFilesService.FileAlreadyExists"), path));
		String errorString = "{\"category\":1,\"rc\":4,\"reason\":19,\"message\":\"The specified file already exists\",\"stack\":\"  1 1A4CE920+0000014C CreatFileServlet::createFile()\\n  2 1A4CE388+000000EE CreatFileServlet::doPost()\\n  3 1A4CE340+00000026 CreatFileServlet::service()\\n  4 1A4FAA18+00000166 TsoServlet::run()\\n  5 1A4FA420+0000035A main\\n  6 095F3EB0+0000127E CEEVROND\\n  7 19CDC0C8+000000B4 EDCZHINV\\n  8 09498170+000001C6 CEEBBEXT\\n\"}";
		
		testInvalidCreate(expected, UssFileType.file, Status.INTERNAL_SERVER_ERROR, errorString);
	}

	@Test
	public void creatingExistingDirectoryShouldThrowException() throws Exception {
		BadRequestException expected = new BadRequestException(String.format(Messages.getString("ZosmfFilesService.DirectoryAlreadyExists"), path));
		String errorString = "{\"category\":1,\"rc\":4,\"reason\":19,\"message\":\"The specified directory already exists\",\"stack\":\"  1 1A4D8D50+0000014C CreatFileServlet::createDir()\\n  2 1A4D8388+000000FE CreatFileServlet::doPost()\\n  3 1A4D8340+00000026 CreatFileServlet::service()\\n  4 1A504A18+00000166 TsoServlet::run()\\n  5 1A504420+0000035A main\\n  6 095F3EB0+0000127E CEEVROND\\n  7 19CDC0C8+000000B4 EDCZHINV\\n  8 09498170+000001C6 CEEBBEXT\\n\"}";

		testInvalidCreate(expected, UssFileType.directory, Status.INTERNAL_SERVER_ERROR, errorString);
	}

	@Test
	public void creatingFileWithNoAccessShouldThrowException() throws Exception {
		ForbiddenException expected = new ForbiddenException(String.format(Messages.getString("ZosmfFilesService.AccessDenied"), path));
		String errorString = "{\"category\":8,\"rc\":-1,\"reason\":98959436,\"message\":\"creat() error\",\"stack\":\"  1 1A4D8920+00000322 CreatFileServlet::createFile()\\n  2 1A4D8388+000000EE CreatFileServlet::doPost()\\n  3 1A4D8340+00000026 CreatFileServlet::service()\\n  4 1A504A18+00000166 TsoServlet::run()\\n  5 1A504420+0000035A main\\n  6 095F3EB0+0000127E CEEVROND\\n  7 19CDC0C8+000000B4 EDCZHINV\\n  8 09498170+000001C6 CEEBBEXT\\n\"}";

		testInvalidCreate(expected, UssFileType.file, Status.INTERNAL_SERVER_ERROR, errorString);
	}

	@Test
	public void creatingDirectoryWithNoAccessShouldThrowException() throws Exception {
		ForbiddenException expected = new ForbiddenException(String.format(Messages.getString("ZosmfFilesService.AccessDenied"), path));
		String errorString = "{\"category\":8,\"rc\":-1,\"reason\":-276865003,\"message\":\"mkdir() error\",\"stack\":\"  1 1A4D8D50+00000322 CreatFileServlet::createDir()\\n  2 1A4D8388+000000FE CreatFileServlet::doPost()\\n  3 1A4D8340+00000026 CreatFileServlet::service()\\n  4 1A504A18+00000166 TsoServlet::run()\\n  5 1A504420+0000035A main\\n  6 095F3EB0+0000127E CEEVROND\\n  7 19CDC0C8+000000B4 EDCZHINV\\n  8 09498170+000001C6 CEEBBEXT\\n\"}";

		testInvalidCreate(expected, UssFileType.directory, Status.INTERNAL_SERVER_ERROR, errorString);
	}
	
	private void testValidCreate(UssFileType type) throws Exception, AtlasException {
		testValidCreate(type, path, null, null);
	}
	
	private void testValidCreate(UssFileType type, String aPath, String numericPermissions, String symbolicPermissions) throws Exception, AtlasException {
		CreateFileRequest request = CreateFileRequest.builder().type(type).path(aPath).build();
		if (numericPermissions != null) {
			request.setPermissions(numericPermissions);
		}

		Response response = mockResponse(Status.CREATED, null);

		String relativeUri = "restfiles/fs" + (aPath.startsWith("/") ? "" : "/") + aPath;
		ZosmfPostFileRequest body = ZosmfPostFileRequest.builder().type(type.toString()).build();
		if (symbolicPermissions != null) {
			body.setMode(symbolicPermissions);
		}
		mockPostRequestResponse(relativeUri, body, response);

		assertEquals(aPath, filesService.createFileOrDirectory(request));
	}
	
	private void testInvalidCreate(Exception expectedException, UssFileType type, Status statusCode, String responseString) throws Exception {
		CreateFileRequest request = CreateFileRequest.builder().type(type).path(path).build();

		Response response = mockResponse(statusCode, responseString);
		
		String relativeUri = "restfiles/fs" + request.getPath();
		ZosmfPostFileRequest body = ZosmfPostFileRequest.builder().type(type.toString()).build();
		mockPostRequestResponse(relativeUri, body, response);

		shouldThrow(expectedException, () -> filesService.createFileOrDirectory(request));
	}
	
	@Test
	public void testDelete() throws Exception {
		Response response = mockResponse(Status.NO_CONTENT, null);
		String relativeUri = "restfiles/fs/" + (path.startsWith("/") ? "" : "/") + path;

		Builder builder = mockClient(relativeUri);
		when(builder.header("X-IBM-Option", "recursive")).thenReturn(builder);
		when(filesService.client.sendRequest(builder, HttpMethod.DELETE)).thenReturn(response);

		filesService.delete(path);
	}
	
	@Test
	public void deletingNonExistingFileShouldThrowException() throws Exception {
		NotFoundException expected = new NotFoundException(String.format(Messages.getString("ZosmfFilesService.FileNotFound"), path));
		String errorString = "{\"category\":8,\"rc\":-1,\"reason\":96141420,\"message\":\"lstat() error\",\"stack\":\"  1 1A4D8B30+000007EE DeleteFileServlet::deleteFile2Dir()\\n  2 1A4D8378+000000A8 DeleteFileServlet::doDelete()\\n  3 1A4D8330+00000026 DeleteFileServlet::service()\\n  4 1A504060+00000166 TsoServlet::run()\\n  5 1A503A68+0000035A main\\n  6 095F3EB0+0000127E CEEVROND\\n  7 19CDC0C8+000000B4 EDCZHINV\\n  8 09498170+000001C6 CEEBBEXT\\n\",\"details\":[\"EDC5129I No such file or directory. (errno2=0x053B006C)\"]}";
		testInvalidDelete(expected, Status.INTERNAL_SERVER_ERROR, errorString);
	}
	
	@Test
	public void deletingFileWithNoAccessShouldThrowException() throws Exception {
		ForbiddenException expected = new ForbiddenException(String.format(Messages.getString("ZosmfFilesService.AccessDenied"), path));
		String errorString = "{\"category\":8,\"rc\":-1,\"reason\":-276865003,\"message\":\"lstat() error\",\"stack\":\"  1 1A4D8B30+000007EE DeleteFileServlet::deleteFile2Dir()\\n  2 1A4D8378+000000A8 DeleteFileServlet::doDelete()\\n  3 1A4D8330+00000026 DeleteFileServlet::service()\\n  4 1A504060+00000166 TsoServlet::run()\\n  5 1A503A68+0000035A main\\n  6 095F3EB0+0000127E CEEVROND\\n  7 19CDC0C8+000000B4 EDCZHINV\\n  8 09498170+000001C6 CEEBBEXT\\n\",\"details\":[\"EDC5111I Permission denied. (errno2=0xEF5F6015)\"]}";
		testInvalidDelete(expected, Status.INTERNAL_SERVER_ERROR, errorString);
	}
	
	private void testInvalidDelete(Exception expectedException, Status statusCode, String responseString) throws Exception {
		Response response = mockResponse(statusCode, responseString);
		
		String relativeUri = "restfiles/fs/" + (path.startsWith("/") ? "" : "/") + path;
		Builder builder = mockClient(relativeUri);
		when(builder.header("X-IBM-Option", "recursive")).thenReturn(builder);
		when(filesService.client.sendRequest(builder, HttpMethod.DELETE)).thenReturn(response);
		
		shouldThrow(expectedException, () -> filesService.delete(path));
	}
	
	@Test
	public void testUpdateFileContentWithChecksum() throws Exception {
		testUpdateFileContent("TestContent", "F4A5A479E78AFD4CFF7DF13937AB82AE");
	}
	
	@Test
	public void testUpdateFileContentWithoutChecksum() throws Exception {
		testUpdateFileContent("TestContent2", null);
	}
	
	private void testUpdateFileContent(String content, String checksum) throws Exception {
		UpdateFileContentsRequest request = UpdateFileContentsRequest.builder().content(content).build();
		if (checksum != null) {
			request.setChecksum(checksum);
		}

		Response response = mockResponse(Status.NO_CONTENT, null);
		String relativeUri = "restfiles/fs/" + (path.startsWith("/") ? "" : "/") + path;
		
		Builder builder = mockClient(relativeUri);
		if (checksum != null) {
			when(builder.header("If-Match", checksum)).thenReturn(builder);
		}
		when(builder.header("X-IBM-Data-Type", "binary")).thenReturn(builder);
		when(builder.header("Content-Type", MediaType.TEXT_PLAIN)).thenReturn(builder);
		when(filesService.client.putRequestWithContent(builder, content, MediaType.TEXT_PLAIN_TYPE)).thenReturn(response);
		mockChTagWebTarget(builder);
		mockGetRequestResponse(builder, response);
		mockChTagResponse(builder);

		filesService.updateFileContent(path, request);
	}

	@Test
	public void updateFileContentWithIncorrectChecksum() throws Exception {
		BadRequestException expected = new BadRequestException(Messages.getString("ZosmfFilesService.ChecksumInvalid"));
		testInvalidUpdateFileContent(expected, Status.PRECONDITION_FAILED, "");
	}
	
	@Test @Ignore("Ignore until we work out wanted behaviour")
	public void updateFileContentWithNonExistingFileShouldThrowException() throws Exception {
	}
	
	@Test
	public void updateFileContentWithNoAccessShouldThrowException() throws Exception {
		ForbiddenException expected = new ForbiddenException(String.format(Messages.getString("ZosmfFilesService.AccessDenied"), path));
		String errorString = "{\"category\":6,\"rc\":8,\"reason\":-277913579,\"message\":\"Client is not authorized for file access.\",\"stack\":\"  1 12BDDC00+000003BA PosixFileControl::open(const char*)\\n  2 12BDC318+000003A8 PutFileServlet::doPut()\\n  3 12BE2E10+000000CA TsoServlet::service()\\n  4 12C0C160+00000166 TsoServlet::run()\\n  5 12C0BB68+0000035A main\\n  6 08B26F20+0000127E CEEVROND\\n  7 123E6E18+000000B4 EDCZHINV\\n  8 089C7190+000001D2 CEEBBEXT\\n\",\"details\":[\"EDC5111I Permission denied. (errno2=0xEF076015)\"]}";
		testInvalidUpdateFileContent(expected, Status.INTERNAL_SERVER_ERROR, errorString);
	}
	
	private void testInvalidUpdateFileContent(Exception expectedException, Status statusCode, String responseString) throws Exception {
		String content = "JUNK";
		UpdateFileContentsRequest request = UpdateFileContentsRequest.builder().content(content).build();
		
		Response response = mockResponse(statusCode, responseString);

		String relativeUri = "restfiles/fs/" + (path.startsWith("/") ? "" : "/") + path;
		
		ZosmfFilesService fileserviceSpy = spy(filesService);
		Builder builder = mockClient(relativeUri);
		when(builder.header("X-IBM-Data-Type", "binary")).thenReturn(builder);
		when(builder.header("Content-Type", MediaType.TEXT_PLAIN)).thenReturn(builder);
		when(fileserviceSpy.client.putRequestWithContent(builder, content, MediaType.TEXT_PLAIN_TYPE)).thenReturn(response);
		mockChTagWebTarget(builder);
		mockGetRequestResponse(builder, response);
		when(fileserviceSpy.createRequest(anyString(), new String[0])).thenReturn(builder);
		mockChTagResponse(builder);

		shouldThrow(expectedException, () -> filesService.updateFileContent(path, request));
	}
	
	@Test
	public void testGetAttributesWorksForDirectory() throws Exception {
		testGetAttributesWorksForDirectory(path);
	}
	
	@Test
	public void testGetAttributesWorksForRoot() throws Exception {
		testGetAttributesWorksForDirectory("/");
	}
	
	private void testGetAttributesWorksForDirectory(String aPath) throws Exception {
		Response response = mockResponse(Status.OK, loadTestFile("zosmfService_getDirectory.json"));
		String relativeUri = "restfiles/fs";
		
		URI dummyURI = new URI("http://ibm.com/dummy/path/junk%2Fpath");
		when(filesService.uriInfo.getAbsolutePath()).thenReturn(dummyURI);
		
		mockRequestResponse(relativeUri, HttpMethod.GET, response, "path", aPath);
		
		List<GetFileChildAttributesResponse> children = new ArrayList<>();
		children.add(createChildObject(aPath, UssFileType.directory,"dir1"));
		children.add(createChildObject(aPath, UssFileType.directory,"dir2"));
		children.add(createChildObject(aPath, UssFileType.file, "file"));
		
		GetFileAttributesResponse expected = GetFileAttributesResponse.builder()
			.type(UssFileType.directory)
			.fileOwner("STEVENH")
			.group("TSOUSER")
			.permissionsSymbolic("rwxr-xr-x")
			.permissionsNumeric("755")
			.size(8192l)
			.children(children)
			.build();
		
		GetFileAttributesResponse attributes = filesService.getAttributes(aPath);
		assertEquals(expected, attributes);

		assertEquals(new Date(1510852812000l), attributes.getLastModifiedDate());
	}
	
	private GetFileChildAttributesResponse createChildObject(String parentPath, UssFileType type, String name) throws URISyntaxException, UnsupportedEncodingException {
		//TODO - refactor URI
		URI childUri = new URI("http://ibm.com/dummy/path/junk%2Fpath" + URLEncoder.encode("/" + name, "UTF-8"));
		return GetFileChildAttributesResponse.builder().name(name).type(UssFileType.directory).link(childUri).build();
	}
	
	@Test
	public void testGetAttributesWorksForFile() throws Exception {
		Response response = mockResponse(Status.OK, loadTestFile("zosmfService_getFile.json"));
		String relativeUri = "restfiles/fs";
		
		UriBuilder uriBuilder = mock(UriBuilder.class);
		when(filesService.uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
		when(uriBuilder.path("content")).thenReturn(uriBuilder);
		URI dummyURI = new URI("http://ibm.com/dummy/path/junk%2Fpath/content");
		when(uriBuilder.build()).thenReturn(dummyURI);
		
		mockRequestResponse(relativeUri, HttpMethod.GET, response, "path", (path.startsWith("/") ? "" : "/") + path);
		
		GetFileAttributesResponse expected = GetFileAttributesResponse.builder()
			.type(UssFileType.file)
			.fileOwner("STEVENH")
			.group("TSOUSER")
			.permissionsSymbolic("rwxr-----")
			.permissionsNumeric("740")
			.size(51l)
			//TODO - later "codepage" : "IBM-1047"
			//TODO - later "contentType": "text"/"binary"/"mixed",
			.content(dummyURI)
			.build();
		
		GetFileAttributesResponse attributes = filesService.getAttributes(path);
		assertEquals(expected, attributes);

		assertEquals(new Date(1511197783000l), attributes.getLastModifiedDate());
	}
	
	@Test
	public void testGetContentsOfFile() throws Exception {
		String content = "test content";
		String checksum = "4E4A3CA39936A8B2ACB874B0317C7E5C";
		Response response = mockResponse(Status.OK, content);
		when(response.getHeaderString("ETag")).thenReturn(checksum);
		
		String relativeUri = "restfiles/fs" + (path.startsWith("/") ? "" : "/") + path;	
		Builder builder = mockClient(relativeUri);
		mockGetRequestResponse(builder, response);
		mockChTagResponse(builder);
		
		GetFileContentResponse expected = GetFileContentResponse.builder().content(content).checksum(checksum).build();
		assertEquals(expected, filesService.getContent(path));
	}
	
	@Test
	public void testGetContentsOfFolderThrowsBadRequest() throws Exception {
		BadRequestException expected = new BadRequestException(String.format(Messages.getString("ZosmfFilesService.GetContentOnDirectory"), path));
		String errorString = "{\"category\":6,\"rc\":8,\"reason\":92078512,\"message\":\"file lock via fcntl() failed\",\"stack\":\"  1 12BE8B88+00000634 PosixFileControl::setLock(short)\\n  2 12BE6318+00000674 GetFileServlet::doGet()\\n  3 12BF2D88+0000008A TsoServlet::service()\\n  4 12C1CB88+00000166 TsoServlet::run()\\n  5 12C1C590+0000035A main\\n  6 08B26F20+0000127E CEEVROND\\n  7 123E6E18+000000B4 EDCZHINV\\n  8 089C7190+000001D2 CEEBBEXT\\n\",\"details\":[\"EDC5121I Invalid argument. (errno2=0x055501B0)\"]}";
		testInvalidGetContent(expected, Status.INTERNAL_SERVER_ERROR, errorString);
	}

	@Test
	public void getContentOnNonExistingFileShouldThrowException() throws Exception {
		NotFoundException expected = new NotFoundException(String.format(Messages.getString("ZosmfFilesService.FileNotFound"), path));
		String errorString = "{\"category\":6,\"rc\":8,\"reason\":98762850,\"message\":\"File not found.\",\"stack\":\"  1 12BE85D8+000001C8 PosixFileControl::open(const char*)\\n  2 12BE6318+0000065E GetFileServlet::doGet()\\n  3 12BF2D88+0000008A TsoServlet::service()\\n  4 12C1CB88+00000166 TsoServlet::run()\\n  5 12C1C590+0000035A main\\n  6 08B26F20+0000127E CEEVROND\\n  7 123E6E18+000000B4 EDCZHINV\\n  8 089C7190+000001D2 CEEBBEXT\\n\",\"details\":[\"EDC5129I No such file or directory. (errno2=0x05620062)\"]}";
		testInvalidGetContent(expected, Status.INTERNAL_SERVER_ERROR, errorString);
	}
	
	@Test
	public void getContentOnFileWithNoAccessShouldThrowException() throws Exception {
		ForbiddenException expected = new ForbiddenException(String.format(Messages.getString("ZosmfFilesService.AccessDenied"), path));
		String errorString = "{\"category\":16,\"rc\":16,\"reason\":1,\"message\":\"stat() failed\",\"stack\":\"  1 12BE6318+000003C4 ListFilesServlet::doGet()\\n  2 12BEAFC0+00000086 TsoServlet::service()\\n  3 12C14050+00000166 TsoServlet::run()\\n  4 12C13A58+0000035A main\\n  5 08B26F20+0000127E CEEVROND\\n  6 123E6E18+000000B4 EDCZHINV\\n  7 089C7190+000001D2 CEEBBEXT\\n\",\"details\":[\"EDC5111I Permission denied. (errno2=0xEF076015)\"]}";
		testInvalidGetContent(expected, Status.INTERNAL_SERVER_ERROR, errorString);
	}
	
	private void testInvalidGetContent(Exception expectedException, Status statusCode, String responseString) throws Exception {
		String relativeUri = "restfiles/fs" + (path.startsWith("/") ? "" : "/") + path;
		Response response = mockResponse(statusCode, responseString);
		Builder builder = mockClient(relativeUri);
		mockGetRequestResponse(builder, response);
		mockChTagResponse(builder);
		shouldThrow(expectedException, () -> filesService.getContent(path));
	}

	@SuppressWarnings("boxing")
	private static Response mockResponse(Status statusCode, String entity) {
		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(statusCode.getStatusCode());
		when(response.readEntity(String.class)).thenReturn(entity);
		return response;
	}
	
	@Test
	public void testBinaryChtagBin1() throws Exception {
		String relativeUri = "restfiles/fs" + (path.startsWith("/") ? "" : "/") + path;	
		String output = "{\"stdout\":[\"t ISO8859-1 T=on /atlas/wlp/usr/servers/Atlas/logs/messages.log\"]}"; //$NON-NLS-1$
		initializeChtag(relativeUri, output);
		assertFalse(filesService.getTag(path).convertRecommended());
	}
	@Test
	public void testBinaryChtag1Bin2() throws Exception {
		String relativeUri = "restfiles/fs" + (path.startsWith("/") ? "" : "/") + path;	
		String output = "{\"stdout\":[\"t ISO8859-1 T=off /atlas/wlp/usr/servers/Atlas/logs/messages.log\"]}"; //$NON-NLS-1$
		initializeChtag(relativeUri, output);
		assertFalse(filesService.getTag(path).convertRecommended());
	}
	@Test
	public void testBinaryChtagText1() throws Exception {
		String relativeUri = "restfiles/fs" + (path.startsWith("/") ? "" : "/") + path;	
		String output = "{\"stdout\":[\"t 016 T=on /atlas/wlp/usr/servers/Atlas/logs/messages.log\"]}"; //$NON-NLS-1$
		initializeChtag(relativeUri, output);
		assertTrue(filesService.getTag(path).convertRecommended());
	}
	private void initializeChtag(String relativeUri, String output) throws Exception {
		JSONObject inputJSON = JSONObject.parse("{\"request\":\"chtag\",\"action\":\"list\"}"); //$NON-NLS-1$
		Builder builder = mockClient(relativeUri); //$NON-NLS-1$  //$NON-NLS-2$ 
		Response response1 = mockResponse(Status.OK, output); 
		when(filesService.client.putRequestWithContent(builder, inputJSON, MediaType.APPLICATION_JSON_TYPE)).thenReturn(response1);
	}
	private Builder mockChTagResponse(Builder builder) throws Exception {
		JSONObject inputJSON = JSONObject.parse("{\"request\":\"chtag\",\"action\":\"list\"}"); //$NON-NLS-1$
		String entity = "{\"stdout\":[\"t 0123 T=on /atlas/wlp/usr/servers/Atlas/logs/messages.log\"]}"; //$NON-NLS-1$
		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(response.readEntity(String.class)).thenReturn(entity);
		mockPutRequestResponse(builder, inputJSON, response);
		return builder;
	}
	private void mockChTagWebTarget(Builder builder) {
		WebTarget webtarget = mock(WebTarget.class);
		when(filesService.client.createTarget(443, CONTEXT_ROOT)).thenReturn(webtarget);
		when(webtarget.path(anyString())).thenReturn(webtarget);
		when(webtarget.request(MediaType.APPLICATION_JSON)).thenReturn(builder);		
	}
}
