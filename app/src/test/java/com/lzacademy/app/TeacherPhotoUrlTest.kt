package com.lzacademy.app

import com.lzacademy.app.components.teacherPhotoUrl
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.models.Teacher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Student-space teacher avatars were rendering initials only because the UI never
 * resolved `teachers.photo_url`. This pins the resolution rule: the column stores
 * `<bucket>/<object>` (e.g. `photos/teachers/<uuid>.jpg?v=…`), never a full URL.
 */
class TeacherPhotoUrlTest {

    private val api = SupabaseApi(projectUrl = "https://example.test", anonKey = "test-key")

    private fun teacher(photoUrl: String?) = Teacher(id = "t1", fullName = "M. Yacine Hamdi", photoUrl = photoUrl)

    @Test
    fun `un chemin relatif devient une url publique du bucket photos`() {
        assertEquals(
            "https://example.test/storage/v1/object/public/photos/teachers/abc.jpg?v=42",
            teacherPhotoUrl(teacher("photos/teachers/abc.jpg?v=42"), api),
        )
    }

    @Test
    fun `une url deja absolue est conservee telle quelle`() {
        assertEquals(
            "https://cdn.example.test/abc.jpg",
            teacherPhotoUrl(teacher("https://cdn.example.test/abc.jpg"), api),
        )
    }

    @Test
    fun `une photo absente ou vide ne produit aucune url`() {
        assertNull(teacherPhotoUrl(teacher(null), api))
        assertNull(teacherPhotoUrl(teacher(""), api))
        assertNull(teacherPhotoUrl(teacher("   "), api))
        assertNull(teacherPhotoUrl(teacher("null"), api))
    }
}
