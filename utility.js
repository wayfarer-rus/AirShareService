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
