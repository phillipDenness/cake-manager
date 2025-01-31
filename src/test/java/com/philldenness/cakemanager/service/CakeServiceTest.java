package com.philldenness.cakemanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.philldenness.cakemanager.dto.CakeDTO;
import com.philldenness.cakemanager.dto.CakeRequest;
import com.philldenness.cakemanager.entity.CakeEntity;
import com.philldenness.cakemanager.mapper.CakeMapper;
import com.philldenness.cakemanager.metrics.CounterManager;
import com.philldenness.cakemanager.metrics.CounterName;
import com.philldenness.cakemanager.repository.CakeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

@ExtendWith(MockitoExtension.class)
class CakeServiceTest {

	@InjectMocks
	private CakeService cakeService;

	@Mock
	private CakeRepository cakeRepository;

	@Mock
	private CakeMapper cakeMapper;

	@Mock
	private CounterManager counterManager;

	// region all cakes
	@Test
	void shouldMapDtoForEachWithEntity() {
		CakeDTO cakeDTO1 = mock(CakeDTO.class);
		CakeDTO cakeDTO2 = mock(CakeDTO.class);
		CakeEntity cakeEntity1 = mock(CakeEntity.class);
		CakeEntity cakeEntity2 = mock(CakeEntity.class);

		when(cakeRepository.findAll()).thenReturn(List.of(cakeEntity1, cakeEntity2));
		when(cakeMapper.toDTO(any(CakeEntity.class)))
				.thenReturn(cakeDTO1)
				.thenReturn(cakeDTO2);

		List<CakeDTO> cakes = cakeService.getCakes();

		verify(cakeMapper, times(1)).toDTO(cakeEntity1);
		verify(cakeMapper, times(1)).toDTO(cakeEntity2);
		assertEquals(List.of(cakeDTO1, cakeDTO2), cakes);
	}

	@Test
	void shouldIncrementFindAllCounter() {
		when(cakeRepository.findAll()).thenReturn(List.of());
		cakeService.getCakes();

		verify(counterManager, times(1)).increment(CounterName.FIND_ALL_COUNTER);
	}

	@Test
	void shouldPropagateExceptionFromFindAll() {
		when(cakeRepository.findAll()).thenThrow(mock(InvalidDataAccessResourceUsageException.class));

		assertThrows(InvalidDataAccessResourceUsageException.class, () -> cakeService.getCakes());
	}

	// endregion

	// region cake by id
	@Test
	void shouldCallRepoWithId() {
		Long id = 1L;
		when(cakeRepository.findById(anyLong())).thenReturn(Optional.of(mock(CakeEntity.class)));

		cakeService.getCakeById(id);

		verify(cakeRepository).findById(id);
	}

	@Test
	void shouldPassEntityToMapper() {
		Long id = 1L;
		CakeDTO expectedCake = mock(CakeDTO.class);
		CakeEntity cakeEntity = mock(CakeEntity.class);
		when(cakeRepository.findById(anyLong())).thenReturn(Optional.of(cakeEntity));
		when(cakeMapper.toDTO(any(CakeEntity.class))).thenReturn(expectedCake);

		CakeDTO cakeDTO = cakeService.getCakeById(id);

		verify(cakeMapper).toDTO(cakeEntity);
		assertEquals(expectedCake, cakeDTO);
	}

	@Test
	void shouldIncrementFindByIdCounter() {
		CakeDTO expectedCake = mock(CakeDTO.class);
		CakeEntity cakeEntity = mock(CakeEntity.class);
		when(cakeRepository.findById(anyLong())).thenReturn(Optional.of(cakeEntity));
		when(cakeMapper.toDTO(any(CakeEntity.class))).thenReturn(expectedCake);

		cakeService.getCakeById(1L);

		verify(counterManager, times(1)).increment(CounterName.FIND_BY_ID_COUNTER);
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenRepoOptionalIsEmpty() {
		when(cakeRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> cakeService.getCakeById(9L));
	}

	@Test
	void shouldPropagateExceptionFromFindById() {
		when(cakeRepository.findById(anyLong())).thenThrow(mock(InvalidDataAccessResourceUsageException.class));

		assertThrows(InvalidDataAccessResourceUsageException.class, () -> cakeService.getCakeById(1L));
	}
	// endregion

	// region create cake
	@Test
	void shouldPassCreateRequestToMapper() {
		CakeEntity cakeEntity = mock(CakeEntity.class);
		CakeEntity savedEntity = mock(CakeEntity.class);
		CakeRequest toSave = mock(CakeRequest.class);
		CakeDTO fromEntity = mock(CakeDTO.class);

		when(cakeMapper.toEntity(toSave)).thenReturn(cakeEntity);
		when(cakeRepository.save(cakeEntity)).thenReturn(savedEntity);
		when(cakeMapper.toDTO(savedEntity)).thenReturn(fromEntity);

		CakeDTO savedCake = cakeService.create(toSave);

		verify(cakeRepository).save(cakeEntity);
		assertEquals(fromEntity, savedCake);
	}

	@Test
	void shouldIncrementSaveCounter() {
		CakeEntity cakeEntity = mock(CakeEntity.class);
		CakeEntity savedEntity = mock(CakeEntity.class);
		CakeRequest toSave = mock(CakeRequest.class);
		CakeDTO fromEntity = mock(CakeDTO.class);

		when(cakeMapper.toEntity(toSave)).thenReturn(cakeEntity);
		when(cakeRepository.save(cakeEntity)).thenReturn(savedEntity);
		when(cakeMapper.toDTO(savedEntity)).thenReturn(fromEntity);

		cakeService.create(toSave);

		verify(counterManager, times(1)).increment(CounterName.SAVE);
	}

	@Test
	void shouldPropagateExceptionFromSave() {
		CakeRequest toSave = mock(CakeRequest.class);
		when(cakeMapper.toEntity(toSave)).thenReturn(mock(CakeEntity.class));
		when(cakeRepository.save(any(CakeEntity.class))).thenThrow(mock(InvalidDataAccessResourceUsageException.class));

		assertThrows(InvalidDataAccessResourceUsageException.class,
				() -> cakeService.create(toSave)
		);
	}
	// endregion

	// region update cake
	@Test
	void shouldIncrementUpdateCounter() {
		Long id = 1L;
		CakeEntity newEntity = mock(CakeEntity.class);
		CakeEntity oldEntity = mock(CakeEntity.class);
		CakeEntity newSavedEntity = mock(CakeEntity.class);
		CakeRequest toSave = mock(CakeRequest.class);
		CakeDTO fromEntity = mock(CakeDTO.class);

		when(cakeMapper.toEntity(toSave)).thenReturn(newEntity);
		when(cakeRepository.findById(id)).thenReturn(Optional.of(oldEntity));
		when(cakeRepository.save(newEntity)).thenReturn(newSavedEntity);
		when(cakeMapper.toDTO(newSavedEntity)).thenReturn(fromEntity);

		CakeDTO savedCake = cakeService.update(id, toSave);

		verify(cakeRepository).save(newEntity);
		assertEquals(fromEntity, savedCake);
	}

	@Test
	void shouldPerformUpdate() {
		Long id = 1L;
		CakeEntity newEntity = mock(CakeEntity.class);
		CakeEntity oldEntity = mock(CakeEntity.class);
		CakeEntity newSavedEntity = mock(CakeEntity.class);
		CakeRequest toSave = mock(CakeRequest.class);
		CakeDTO fromEntity = mock(CakeDTO.class);

		when(cakeMapper.toEntity(toSave)).thenReturn(newEntity);
		when(cakeRepository.findById(id)).thenReturn(Optional.of(oldEntity));
		when(cakeRepository.save(newEntity)).thenReturn(newSavedEntity);
		when(cakeMapper.toDTO(newSavedEntity)).thenReturn(fromEntity);

		cakeService.update(id, toSave);

		verify(counterManager, times(1)).increment(CounterName.UPDATE);
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenUpdateIdIsNotFound() {
		when(cakeRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> cakeService.update(9L, mock(CakeRequest.class)));
	}

	@Test
	void shouldPropagateExceptionWhenFindByIdThrowsException() {
		when(cakeRepository.findById(anyLong())).thenThrow(mock(InvalidDataAccessResourceUsageException.class));

		assertThrows(InvalidDataAccessResourceUsageException.class, () -> cakeService.update(1L, mock(CakeRequest.class)));
	}

	@Test
	void shouldPropagateExceptionWhenSaveThrowsException() {
		CakeEntity newEntity = mock(CakeEntity.class);
		CakeEntity oldEntity = mock(CakeEntity.class);
		CakeRequest toSave = mock(CakeRequest.class);

		when(cakeMapper.toEntity(any(CakeRequest.class))).thenReturn(newEntity);
		when(cakeRepository.findById(anyLong())).thenReturn(Optional.of(oldEntity));
		when(cakeRepository.save(any(CakeEntity.class))).thenThrow(mock(InvalidDataAccessResourceUsageException.class));

		assertThrows(InvalidDataAccessResourceUsageException.class, () -> cakeService.update(1L, toSave));
	}
	// endregion

	// region delete cake
	@Test
	void shouldCallRepoDeleteByIdWithSuppliedId() {
		Long id = 1L;
		when(cakeRepository.existsById(anyLong())).thenReturn(true);

		cakeService.delete(id);

		verify(cakeRepository).deleteById(id);
	}

	@Test
	void shouldIncrementDeleteCounter() {
		when(cakeRepository.existsById(anyLong())).thenReturn(true);

		cakeService.delete(1L);

		verify(counterManager).increment(CounterName.DELETE);
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenDeleteIdIsNotFound() {
		when(cakeRepository.existsById(anyLong())).thenReturn(false);

		assertThrows(IllegalArgumentException.class, () -> cakeService.delete(9L));
	}

	@Test
	void shouldPropagateExceptionFromDelete() {
		Long id = 1L;
		when(cakeRepository.existsById(anyLong())).thenReturn(true);
		doThrow(mock(InvalidDataAccessResourceUsageException.class)).when(cakeRepository).deleteById(anyLong());

		assertThrows(InvalidDataAccessResourceUsageException.class,
				() -> cakeService.delete(id)
		);
	}

	@Test
	void shouldPropagateExceptionFromExistsById() {
		Long id = 1L;
		when(cakeRepository.existsById(anyLong())).thenThrow(mock(InvalidDataAccessResourceUsageException.class));

		assertThrows(InvalidDataAccessResourceUsageException.class,
				() -> cakeService.delete(id)
		);
	}
	// endregion
}