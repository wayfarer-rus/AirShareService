// file browsing samples
function getFilesList(folder) {
    var files = [];
    var allFiles = fs.readdirSync(folder);

    for (var i = 0; i < allFiles.length; ++i) {
        var file = allFiles[i];

        if (file[0] !== '.') {
            var filePath = folder + '/' + file;
            var fileStat = fs.statSync(filePath);

            if (fileStat.isFile()) {
                files.push(file);
            }
        }
    }

    return files;
}

function getFolderList(folder) {
    var dirs = [];
    var allFiles = fs.readdirSync(folder);

    for (var i = 0; i < allFiles.length; ++i) {
        var file = allFiles[i];

        if (file[0] !== '.') {
            var filePath = folder + '/' + file;
            var fileStat = fs.statSync(filePath);

            if (fileStat.isDirectory()) {
                dirs.push(file);
            }
        }
    }

    return dirs;
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
