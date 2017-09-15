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
                        "File-Name" : encodeURI(pathModule.basename(path))
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
                            var zipFileStat = fs.statSync(zipTmpFile)

                            res.writeHeader(200, 
                                {"Content-Type" : "folder",
                                "Content-Length" : zipFileStat.size,
                                "File-Name" : encodeURI(pathModule.basename(zipTmpFile)),
                                "Folder-Name" : encodeURI(pathModule.basename(path))
                            });

                            fReadStream.pipe(res).on('finish', function() { deleteZipTmp(zipTmpFile);} );
                        }
                    });
                }
            }
        });
    }
    else {
        var folder = rootFolder;

        if (req.url !== '/') folder = folder + decodeURI(req.url);

        console.log("trying to open folder: " + folder);

        try {
            var fileStat = fs.statSync(folder);

            if (fileStat.isFile()) {
                res.writeHead(500);
                res.write("File can be downloaded with 'Download' request");
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
