package dev.jvmguard.common.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import dev.jvmguard.agent.config.base.AbstractEntity
import dev.jvmguard.common.Loggers
import dev.jvmguard.data.base.StoredConfig
import dev.jvmguard.data.base.StoredType
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.actions.TriggerAction
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.stereotype.Component
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.NamedType
import java.sql.Connection
import java.sql.Statement
import kotlin.reflect.KClass
import javax.sql.DataSource

@Component
@DependsOnDatabaseInitialization
class ConfigStorage(private val dataSource: DataSource) {

    fun <T : StoredConfig> load(clazz: Class<T>, id: Long): T? {
        var json: String? = null
        try {
            dataSource.connection.use { connection ->
                @Suppress("SqlNoDataSourceInspection")
                connection.prepareStatement("select content from $TABLE where bean_type=? and id=?").use { statement ->
                    statement.setString(1, beanType(clazz))
                    statement.setLong(2, id)
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            json = resultSet.getString("content")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not load config bean {}", clazz.simpleName, e)
        }
        return fromJson(clazz, json, id)
    }

    fun <T : StoredConfig> list(clazz: Class<T>): List<T> {
        val ret = ArrayList<T>()
        try {
            dataSource.connection.use { connection ->
                @Suppress("SqlNoDataSourceInspection")
                connection.prepareStatement("select content, id from $TABLE where bean_type=?").use { statement ->
                    statement.setString(1, beanType(clazz))
                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            fromJson(clazz, resultSet.getString("content"), resultSet.getLong("id"))?.let { ret.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not list config beans {}", clazz.simpleName, e)
        }
        return ret
    }

    fun <T : StoredConfig> store(clazz: Class<T>, bean: T) {
        try {
            dataSource.connection.use { connection ->
                store(connection, clazz, bean)
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not store config bean {}", clazz.simpleName, e)
        }
    }

    private fun <T : StoredConfig> store(connection: Connection, clazz: Class<T>, bean: T) {
        val json = toJson(bean)
        if (bean.id == null) {
            @Suppress("SqlNoDataSourceInspection")
            connection.prepareStatement("insert into $TABLE (bean_type, content) values (?,?)", Statement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setString(1, beanType(clazz))
                statement.setString(2, json)
                statement.execute()
                statement.generatedKeys.use { resultSet ->
                    if (resultSet.next()) {
                        bean.id = resultSet.getLong(1)
                    }
                }
            }
        } else {
            @Suppress("SqlNoDataSourceInspection")
            connection.prepareStatement("update $TABLE set content=? where id=?").use { statement ->
                statement.setString(1, json)
                statement.setLong(2, bean.id!!)
                statement.execute()
            }
        }
    }

    fun <T : StoredConfig> remove(clazz: Class<T>, id: Long) {
        try {
            dataSource.connection.use { connection ->
                @Suppress("SqlNoDataSourceInspection")
                connection.prepareStatement("delete from $TABLE where bean_type=? and id=?").use { statement ->
                    statement.setString(1, beanType(clazz))
                    statement.setLong(2, id)
                    statement.execute()
                }
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not remove config bean {}", clazz.simpleName, e)
        }
    }

    fun <T : StoredConfig> removeAll(clazz: Class<T>) {
        try {
            dataSource.connection.use { connection ->
                removeAll(connection, clazz)
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not remove config beans {}", clazz.simpleName, e)
        }
    }

    private fun <T : StoredConfig> removeAll(connection: Connection, clazz: Class<T>) {
        @Suppress("SqlNoDataSourceInspection")
        connection.prepareStatement("delete from $TABLE where bean_type=?").use { statement ->
            statement.setString(1, beanType(clazz))
            statement.execute()
        }
    }

    fun <T : StoredConfig> replaceAll(clazz: Class<T>, beans: Collection<T>) {
        try {
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    removeAll(connection, clazz)
                    for (bean in beans) {
                        bean.id = null
                        store(connection, clazz, bean)
                    }
                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not replace config beans {}", clazz.simpleName, e)
        }
    }

    private fun <T : StoredConfig> fromJson(beanClass: Class<T>, json: String?, id: Long): T? {
        if (json.isNullOrEmpty()) {
            return null
        }
        return try {
            val ret = OBJECT_MAPPER.readValue(json, beanClass)
            ret.id = id
            ret
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not parse config bean {}", beanClass.simpleName, e)
            null
        }
    }

    // The mixin suppresses the transient fields that live on AbstractEntity and are shared by every stored bean
    @JsonIgnoreProperties("modified", "changeListeners")
    private abstract class StoredBeanMixin

    companion object {
        private val SERVER_LOGGER = Loggers.SERVER

        private const val TABLE = "config_storage"

        private val OBJECT_MAPPER: ObjectMapper = JsonMapper.builder()
            .addModule(CodecEntityJacksonModule())
            .registerSubtypes(*(sealedSubtypes(Trigger::class) + sealedSubtypes(TriggerAction::class)).toTypedArray())
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            .disable(EnumFeature.READ_ENUMS_USING_TO_STRING)
            .addMixIn(AbstractEntity::class.java, StoredBeanMixin::class.java)
            .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
            .changeDefaultVisibility { vc ->
                vc.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            }
            .build()

        fun objectMapper(): ObjectMapper = OBJECT_MAPPER

        private fun sealedSubtypes(root: KClass<*>): List<NamedType> =
            root.sealedSubclasses.flatMap { sub ->
                if (sub.isAbstract) sealedSubtypes(sub) else listOf(NamedType(sub.java, sub.simpleName))
            }

        private fun <T : StoredConfig> beanType(clazz: Class<T>): String {
            val storedType = clazz.getAnnotation(StoredType::class.java)
                ?: throw IllegalArgumentException("Missing @StoredType annotation on " + clazz.name)
            return storedType.value
        }

        private fun toJson(bean: Any): String = OBJECT_MAPPER.writeValueAsString(bean)
    }
}
