package data

data class RepositoryData(val author: String, val name: String) {
    override fun toString(): String = "$author/$name"
}