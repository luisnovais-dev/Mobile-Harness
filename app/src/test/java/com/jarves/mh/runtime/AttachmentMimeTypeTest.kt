package com.jarves.mh.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

/** F5: anexo de pasta externa (Uri de arquivo, sem tipo do provedor) usa a extensão, como o anexo normal. */
class AttachmentMimeTypeTest {
    @Test
    fun pngFromFileUriUsesExtensionWhenResolverHasNoType() {
        assertEquals("image/png", attachmentMimeType(null, "png"))
    }

    @Test
    fun jpgExtensionMapsToJpeg() {
        assertEquals("image/jpeg", attachmentMimeType(null, "jpg"))
    }

    @Test
    fun resolverTypeWinsWhenPresent() {
        assertEquals("text/plain", attachmentMimeType("text/plain", "txt"))
    }

    @Test
    fun unknownExtensionStaysOctetStream() {
        assertEquals("application/octet-stream", attachmentMimeType(null, "xyz"))
    }
}
