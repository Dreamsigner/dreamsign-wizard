package com.duolingo.app.data

import kotlin.test.*

class CourseResourcesTest {
    @Test fun everyPublishedExerciseResolvesToItsOwnSourceSession() {
        SampleData.sampleSubjects.flatMap { it.units }.forEach { course ->
            assertTrue(CourseResources.forCourse(course.id).isNotEmpty(), course.id)
            course.lessons.forEach { lesson ->
                val resources = CourseResources.forSession(course.id, assertNotNull(lesson.sourceSessionNumber))
                assertTrue(resources.isNotEmpty(), lesson.id)
                assertTrue(resources.all { it.url.startsWith("https://") })
                lesson.exercises.forEach { exercise -> assertEquals(resources, CourseResources.forExercise(exercise.id), exercise.id) }
            }
        }
    }

    @Test fun unknownIdsDoNotBorrowAnotherCoursesReferences() {
        assertTrue(CourseResources.forCourse("unknown").isEmpty())
        assertTrue(CourseResources.forSession("unknown", 1).isEmpty())
        assertTrue(CourseResources.forExercise("unknown").isEmpty())
    }
}
