/*
 * Copyright 2006-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.samples.todolist;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.testng.TestNGCitrusTestDesigner;
import com.consol.citrus.ftp.client.FtpClient;
import com.consol.citrus.ftp.message.FtpMessage;
import com.consol.citrus.ftp.model.*;
import com.consol.citrus.ftp.server.FtpServer;
import com.consol.citrus.util.FileUtils;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.ftpserver.ftplet.DataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Christoph Deppisch
 */
public class TodoListIT extends TestNGCitrusTestDesigner {

    @Autowired
    private FtpClient ftpClient;

    @Autowired
    private FtpServer ftpServer;

    @Test
    @CitrusTest
    public void testStoreAndRetrieveFile() throws IOException {
        echo("Remove ftp user directory if present");

        action(new DeleteFtpFilesAction("target/ftp/user/citrus/todo"));

        echo("Create new directory on server");

        send(ftpClient)
            .message(FtpMessage.command(FTPCmd.MKD).arguments("todo"));

        receive(ftpClient)
            .message(FtpMessage.result(getMkdirsCommandResult("todo")));

        echo("Directory 'todo' created on FTP server");
        echo("Store file to directory");

        send(ftpClient)
                .fork(true)
                .message(FtpMessage.put("classpath:todo/entry.json", "todo/todo.json", DataType.ASCII));

        receive(ftpServer)
                .message(FtpMessage.command(FTPCmd.STOR).arguments("todo/todo.json"));

        send(ftpServer)
                .payload(FtpMessage.success().getPayload(String.class));

        receive(ftpClient)
                .message(FtpMessage.result(getStoreFileCommandResult()));

        echo("List files in directory");

        send(ftpClient)
                .fork(true)
                .message(FtpMessage.list("todo"));

        receive(ftpServer)
                .message(FtpMessage.command(FTPCmd.LIST).arguments("todo"));

        send(ftpServer)
                .payload(FtpMessage.success().getPayload(String.class));

        receive(ftpClient)
                .message(FtpMessage.result(getListCommandResult("todo.json")));

        echo("Retrieve file from server");

        send(ftpClient)
                .fork(true)
                .message(FtpMessage.get("todo/todo.json", "target/todo/todo.json", DataType.ASCII));

        receive(ftpServer)
                .message(FtpMessage.command(FTPCmd.RETR).arguments("todo/todo.json"));

        send(ftpServer)
                .payload(FtpMessage.success().getPayload(String.class));

        receive(ftpClient)
                .message(FtpMessage.result(getRetrieveFileCommandResult("target/todo/todo.json", new ClassPathResource("todo/entry.json"))));
    }

    private CommandResult getMkdirsCommandResult(String path) {
        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setReplyCode(String.valueOf(257));
        result.setReplyString(String.format("@contains(\"/%s\" created)@", path));

        return result;
    }

    private PutCommandResult getStoreFileCommandResult() {
        PutCommandResult result = new PutCommandResult();
        result.setSuccess(true);
        result.setReplyCode(String.valueOf(226));
        result.setReplyString("@contains(Transfer complete)@");

        return result;
    }

    private GetCommandResult getRetrieveFileCommandResult(String path, Resource content) throws IOException {
        GetCommandResult result = new GetCommandResult();
        result.setSuccess(true);
        result.setReplyCode(String.valueOf(226));
        result.setReplyString("@contains('Transfer complete')@");

        GetCommandResult.File entryResult = new GetCommandResult.File();
        entryResult.setPath(path);
        entryResult.setData(FileUtils.readToString(content));
        result.setFile(entryResult);

        return result;
    }

    private ListCommandResult getListCommandResult(String ... fileNames) {
        ListCommandResult result = new ListCommandResult();
        result.setSuccess(true);
        result.setReplyCode(String.valueOf(226));
        result.setReplyString("@contains('Closing data connection')@");

        ListCommandResult.Files expectedFiles = new ListCommandResult.Files();

        for (String fileName : fileNames) {
            ListCommandResult.Files.File entry = new ListCommandResult.Files.File();
            entry.setPath(fileName);
            expectedFiles.getFiles().add(entry);
        }

        result.setFiles(expectedFiles);

        return result;
    }

}
