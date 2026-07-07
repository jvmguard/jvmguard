package com.jvmguard.common.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.jvmguard.agent.config.base.AbstractEntity
import com.jvmguard.common.Loggers
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.base.StoredType
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.stereotype.Component
import tools.jackson.databind.DatabindContext
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.JavaType
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.PolymorphicTypeValidator
import java.sql.Statement
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
        val json = toJson(bean)
        if (bean.id == null) {
            try {
                dataSource.connection.use { connection ->
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
                }
            } catch (e: Exception) {
                SERVER_LOGGER.error("could not insert config bean {}", clazz.simpleName, e)
            }
        } else {
            try {
                dataSource.connection.use { connection ->
                    @Suppress("SqlNoDataSourceInspection")
                    connection.prepareStatement("update $TABLE set content=? where id=?").use { statement ->
                        statement.setString(1, json)
                        statement.setLong(2, bean.id!!)
                        statement.execute()
                    }
                }
            } catch (e: Exception) {
                SERVER_LOGGER.error("could not update config bean {}", clazz.simpleName, e)
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
                @Suppress("SqlNoDataSourceInspection")
                connection.prepareStatement("delete from $TABLE where bean_type=?").use { statement ->
                    statement.setString(1, beanType(clazz))
                    statement.execute()
                }
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not remove config beans {}", clazz.simpleName, e)
        }
    }

    fun <T : StoredConfig> replaceAll(clazz: Class<T>, beans: Collection<T>) {
        removeAll(clazz)
        for (bean in beans) {
            bean.id = null
            store(clazz, bean)
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

        // Default typing is required because the stored graphs are polymorphic
        private val PERMISSIVE_TYPE_VALIDATOR: PolymorphicTypeValidator = object : PolymorphicTypeValidator.Base() {
            override fun validateBaseType(ctx: DatabindContext, baseType: JavaType): Validity = Validity.ALLOWED

            override fun validateSubClassName(ctx: DatabindContext, baseType: JavaType, subClassName: String): Validity = Validity.ALLOWED

            override fun validateSubType(ctx: DatabindContext, baseType: JavaType, subType: JavaType): Validity = Validity.ALLOWED
        }

        private val OBJECT_MAPPER: ObjectMapper = JsonMapper.builder()
            .activateDefaultTyping(PERMISSIVE_TYPE_VALIDATOR, DefaultTyping.NON_FINAL)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .addMixIn(AbstractEntity::class.java, StoredBeanMixin::class.java)
            .changeDefaultVisibility { vc ->
                vc
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            }
            .build()

        fun objectMapper(): ObjectMapper = OBJECT_MAPPER

        private fun <T : StoredConfig> beanType(clazz: Class<T>): String {
            val storedType = clazz.getAnnotation(StoredType::class.java)
                ?: throw IllegalArgumentException("Missing @StoredType annotation on " + clazz.name)
            return storedType.value
        }

        private fun toJson(bean: Any): String = OBJECT_MAPPER.writeValueAsString(bean)
    }
}
