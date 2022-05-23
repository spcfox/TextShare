package ru.spcfox.sharetext.sharetext.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.spcfox.sharetext.sharetext.model.User

interface UserRepository : JpaRepository<User, Long>