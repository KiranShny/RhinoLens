package io.github.kiranshny.rhinolens.shared.orchestrator

class LruCache<K, V>(private val maxSize: Int) {

    private val map = LinkedHashMap<K, V>(0, 0.75f, true)

    fun get(key: K): V? = map[key]

    fun put(key: K, value: V): V? {
        val previous = map.put(key, value)
        while (map.size > maxSize) {
            val eldest = map.entries.iterator().next()
            map.remove(eldest.key)
        }
        return previous
    }

    fun size(): Int = map.size

    fun clear() = map.clear()
}
