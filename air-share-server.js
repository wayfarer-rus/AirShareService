"use strict"

// Optional. You will see this name in eg. 'ps' or 'top' command
process.title = 'node-airshare-server';

var http = require('http');
var url = require('url');
var fs = require('fs');
var pathModule = require('path');
var formidable = require('formidable');
var unzipModule = require('unzip-stream');
var zipFolder = require('zip-folder');
//var events = require('events');
//var eventEmitter = new events.EventEmitter();

//Assign the event handler to an event:
//eventEmitter.on('close', deleteZipTmp);

var rootFolder = "/Users/wayfarer";
var tmpZipFolder = "/Users/wayfarer/tmp/";
var skipHidden = true;

http.createServer(function (req, res) {
    var q = url.parse(req.url, true);
    console.log(q.pathname);

    if (q.pathname == '/favicon.ico') {
        res.writeHead(404);
        res.end();
    }
    else if (q.pathname == '/__upload__' && req.method.toLowerCase() == 'post') {
        var form = new formidable.IncomingForm();
        form.parse(req, function (err, fields, files) {
            if (err) {
                console.log(err);
                res.writeHead(500);
                res.write(JSON.stringify(err));
                res.end();
            }
            else {
                var wherePath = decodeURI(q.query.path);

                if (wherePath[0] != '/') wherePath = '/'+wherePath;
                if (wherePath[wherePath.length-1]) wherePath += '/';
                var unzip = q.query.unzip;

                for (var key in files) {
                    console.log("----");
                    console.log("key = " + key);
                    console.log("file = " + JSON.stringify(files[key]));
                    console.log("file path = " + files[key].path);
                    console.log("file name = " + files[key].name);
                    var oldpath = files[key].path;
                    var newpath = rootFolder + wherePath + files[key].name;
                    console.log("new path = " + newpath);

                    fs.rename(oldpath, newpath, function (err) {
                        if (err) {
                            res.writeHead(500);
                            res.end(JSON.stringify(err));
                        }
                        else if (unzip) {
                            fs.createReadStream(newpath)
                                .pipe(unzipModule.Extract({ path: pathModule.dirname(newpath) }))
                                .on('close',  function() {
                                    deleteZipTmp(newpath);
                                });
                        }
                    });
                }

                res.writeHead(200);
                res.write('File uploaded and moved!');
                res.end();
                //console.log(files.filetoupload.path);
                //console.log(files.filetoupload.name);
                //console.log(JSON.stringify(files));
            }
        });
    }
    else if (q.pathname == '/__download__') {
        var path = decodeURI(q.query.path);
        console.log('Download : ' + path);

        if (path)
            path = rootFolder + path;

        fs.stat(path, (err, stats) => {
            if (err) {
                res.writeHead(500);
                res.end();
            } else {
                if (stats.isFile()) {
                    res.writeHeader(200,
                        {"Content-Length":stats.size, 
                        "Content-Type": "file",
                        "File-Name" : pathModule.basename(path)
                    });
                    var fReadStream = fs.createReadStream(path);
                    fReadStream.pipe(res);
                } else if (stats.isDirectory()) {
                    var zipTmpFile = tmpZipFolder + "data_" + Date.now() + ".zip";

                    zipFolder(path, zipTmpFile, function(err) {
                        if(err) {
                            console.log('Zip folder failed!', err);
                            res.writeHead(500);
                            res.end();
                        } else {
                            var fReadStream = fs.createReadStream(zipTmpFile);
                            res.writeHeader(200, 
                                {"Content-Type" : "folder",
                                "File-Name" : pathModule.basename(zipTmpFile),
                                "Folder-Name" : pathModule.basename(path)
                            });
                            fReadStream.pipe(res).on('finish', function() { deleteZipTmp(zipTmpFile);} );
                        }
                    });
                }
            }
        });

        /*download(path, function(err, data) {
            if (err) {
                res.writeHead(500);
                res.end(JSON.stringify(err));
            }
            else {
                res.writeHead(200);
                res.end(JSON.stringify(data));
            }
        });*/
    }
    else {
        var folder = rootFolder;

        if (req.url !== '/') folder = folder + decodeURI(req.url);

        console.log("trying to open folder: " + folder);

        try {
            var fileStat = fs.statSync(folder);

            if (fileStat.isFile()) {
                res.writeHead(200);
                res.write(fs.readFileSync(folder));
            }
            else {
                var list = returnDirectory(folder);
                console.log(list);
                res.writeHead(200, {'Content-Type': 'text/json'});

                if (list) res.write(list);
            }
        } catch (any) {
            res.writeHead(404);
        }

        res.end();
    }
}).listen(8080);

function deleteZipTmp(path) {
    fs.unlink(path, (err) => {
        if (err) {
            console.log(err);
        }
        else {
            console.log("tmp zip file " + path + " deleted");
        }
    });
}

function download(path, callback) {
    fs.stat(path, (err, stats) => {
        if (err) {
            callback(err, null);
        }
        else {
            try {
                if (stats.isFile()) {
                    var result = readFile(path);
                    result['type'] = "file";
                    callback(null, result);
                }
                else if (stats.isDirectory()) {
                    // recursivly read all files and folders.
                    // build tree hierarhy
                    var result = readDir(path);
                    result['type'] = "folder";
                    callback(null, result);
                }
            } catch (err) {
                callback(err, null);
            }
        }
    });
}

function returnDirectory(folder) {
    var result = {folders: [], files:[]};
    var filesAndFolders = getFilesAndFolders(folder);

    if (folder != rootFolder) result.folders.push('..');

    filesAndFolders.foldersList.forEach(
        function(elem) {
            result.folders.push(elem);
        }
    );

    filesAndFolders.filesList.forEach(
        function(elem) {
            result.files.push(elem);
        }
    );

    return JSON.stringify(result);
}

function getFilesAndFolders(folder) {
    var files = [];
    var folders = [];
    var allFiles = fs.readdirSync(folder);

    for (var i = 0; i < allFiles.length; ++i) {
        var file = allFiles[i];

        if (file[0] === '.' && skipHidden) {
            continue;
        }
        else {
            var filePath = folder + '/' + file;
            var fileStat = fs.statSync(filePath);

            if (fileStat.isFile()) {
                files.push(file);
            }
            else if (fileStat.isDirectory()) {
                folders.push(file);
            }
        }
    }

    return {filesList:files, foldersList:folders};
}

function readFile(path) {
    var result = {file:{}};
    var data = fs.readFileSync(path);
    result.file['path'] = pathModule.basename(path);
    result.file['data'] = data;

    return result;
}

function readDir(path) {
    var result = {folder:{}};
    var filesAndFolders = getFilesAndFolders(path);
    result.folder['path'] = pathModule.basename(path);

    filesAndFolders.filesList.forEach(
        function(file) {
            var fData = readFile(path + '/' + file);
            if (!result.folder.files)
                result.folder['files'] = [];
            result.folder.files.push(fData);
        }
    );

    filesAndFolders.foldersList.forEach(
        function(folder) {
            var fData = readDir(path + '/' + folder);
            if (!result.folder.folders)
                result.folder['folders'] = [];
            result.folder.folders.push(fData);
        }
    );

    return result;
}
