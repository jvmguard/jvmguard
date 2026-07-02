package com.jvmguard.data.user

import com.jvmguard.common.config.ConfigStorage
import com.jvmguard.common.helper.ListModification
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import javax.security.auth.login.CredentialException

@Component
class UserManager(private val configStorage: ConfigStorage) {

    private val loginNameToUser = HashMap<String, User>()
    private val idToUser = Long2ObjectOpenHashMap<User>()

    @PostConstruct
    fun postConstruct() {
        for (user in configStorage.list(User::class.java)) {
            loginNameToUser[user.loginName] = user
            idToUser.put(user.id!!, user)
        }
    }

    @Synchronized
    fun modifyUsers(listModification: ListModification<User>) {
        for (user in listModification.removedItems) {
            user.id?.let { id ->
                configStorage.remove(User::class.java, id)
                idToUser.remove(id)
            }
            loginNameToUser.remove(user.loginName)
        }
        for (user in listModification.modifiedOrNewItems()) {
            store(user)
        }
    }

    @Synchronized
    fun removeAll() {
        configStorage.removeAll(User::class.java)
        loginNameToUser.clear()
        idToUser.clear()
    }

    @Synchronized
    fun getAllUsers(): Collection<User> = loginNameToUser.values.map { it.clone() }

    @Synchronized
    fun store(user: User) {
        val id = user.id
        if (id != null) {
            val previousUser = idToUser.get(id)
            if (previousUser != null && previousUser.loginName != user.loginName) {
                if (loginNameToUser.containsKey(user.loginName)) {
                    throw CredentialException("The login name already exists")
                }
                loginNameToUser.remove(previousUser.loginName)
            }
        } else if (loginNameToUser.containsKey(user.loginName)) {
            throw CredentialException("The login name already exists")
        }
        configStorage.store(User::class.java, user)
        loginNameToUser[user.loginName] = user
        idToUser.put(user.id!!, user)
    }

    @Synchronized
    fun getByLoginName(loginName: String): User? = loginNameToUser[loginName]?.clone()
}
