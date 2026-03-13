package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.domain.repository.ReaderRepository
import javax.inject.Inject

class GetReaderPageUseCase @Inject constructor(
    private val readerRepository: ReaderRepository
) {

    suspend operator fun invoke(
        bookId: Int,
        page: Int
    ): List<ReaderBlock> {

        return readerRepository.getPageBlocks(
            bookId = bookId,
            page = page
        )
    }
}