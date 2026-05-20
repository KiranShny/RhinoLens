package io.github.kiranshny.rhinolens.shared.orchestrator

class LruCache<K, V>(private val maxSize: Int) {

    private val map = LinkedHashMap<K, V>()

    fun get(key: K): V? {
        val value = map.remove(key) ?: return null
        map[key] = value
        return value
    }

    fun put(key: K, value: V): V? {
        val previous = map.remove(key)
        map[key] = value
        while (map.size > maxSize) {
            val eldest = map.entries.iterator().next().key
            map.remove(eldest)
        }
        return previous
    }

    fun size(): Int = map.size

    fun clear() = map.clear()
}
