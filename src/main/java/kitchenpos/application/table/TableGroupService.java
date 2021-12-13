package kitchenpos.application.table;

import kitchenpos.application.order.OrderService;
import kitchenpos.domain.table.OrderTable;
import kitchenpos.domain.table.OrderTableRepository;
import kitchenpos.domain.table.TableGroup;
import kitchenpos.domain.table.TableGroupRepository;
import kitchenpos.dto.table.OrderTableDto;
import kitchenpos.dto.table.TableGroupDto;
import kitchenpos.exception.order.HasNotCompletionOrderException;
import kitchenpos.exception.table.HasOtherTableGroupException;
import kitchenpos.exception.table.NotEmptyOrderTableException;
import kitchenpos.exception.table.NotGroupingOrderTableCountException;
import kitchenpos.exception.table.NotRegistedMenuOrderTableException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TableGroupService {
    private final OrderService orderService;
    private final OrderTableRepository orderTableRepository;
    private final TableGroupRepository tableGroupRepository;

    public TableGroupService(
        final OrderService orderService, 
        final OrderTableRepository orderTableRepository, 
        final TableGroupRepository tableGroupRepository
    ) {
        this.orderService = orderService;
        this.orderTableRepository = orderTableRepository;
        this.tableGroupRepository = tableGroupRepository;
    }

    @Transactional
    public TableGroupDto create(final TableGroupDto tableGroup) {
        final List<Long> orderTableIds = tableGroup.getOrderTables().stream()
                                                    .map(OrderTableDto::getId)
                                                    .collect(Collectors.toList());

        final List<OrderTable> savedOrderTables = orderTableRepository.findAllByIdIn(orderTableIds);

        validationOfCreate(tableGroup.getOrderTables(), savedOrderTables);

        return TableGroupDto.of(tableGroupRepository.save(TableGroup.of(savedOrderTables)));
    }

    private void validationOfCreate(final List<OrderTableDto> orderTables, final List<OrderTable> savedOrderTables) {
        checkOrderTableSize(orderTables);
        checkAllExistOfOrderTables(orderTables, savedOrderTables);
        for (final OrderTable orderTable : savedOrderTables) {
            checkHasTableGroup(orderTable);
            checkNotEmptyTable(orderTable);
        }
    }

    private void checkHasTableGroup(final OrderTable orderTable) {
        if (orderTable.hasTableGroup()) {
            throw new HasOtherTableGroupException();
        }
    }

    private void checkNotEmptyTable(final OrderTable orderTable) {
        if (!orderTable.isEmpty()) {
            throw new NotEmptyOrderTableException();
        }
    }
    
    private void checkAllExistOfOrderTables(final List<OrderTableDto> orderTables, final List<OrderTable> savedOrderTables) {
        if (orderTables.size() != savedOrderTables.size()) {
            throw new NotRegistedMenuOrderTableException();
        }
    }

    private void checkOrderTableSize(final List<OrderTableDto> orderTables) {
        if (CollectionUtils.isEmpty(orderTables) || orderTables.size() < 2) {
            throw new NotGroupingOrderTableCountException();
        }
    }

    @Transactional
    public void ungroup(final Long tableGroupId) {
        final List<OrderTable> orderTables = orderTableRepository.findAllByTableGroupId(tableGroupId);

        final List<Long> orderTableIds = orderTables.stream()
                                                    .map(OrderTable::getId)
                                                    .collect(Collectors.toList());

        validationOfUpgroup(orderTableIds);

        for (OrderTable orderTable : orderTables) {
            orderTable.unGroupTable();
        }
    }

    private void validationOfUpgroup(final List<Long> orderTableIds) {
        if (orderService.isExistNotCompletionOrder(orderTableIds)) {
            throw new HasNotCompletionOrderException();
        }
    }
}
