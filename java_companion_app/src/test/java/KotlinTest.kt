fun main(args: Array<String>) {
    var a1 = Array(2, init = {i ->
        Integer.valueOf(i)
    })

    a1 = a1.plus(Array(3, init = {i ->
        Integer.valueOf(i*10)
    }))

    a1.forEach { print("$it; ") }
}

