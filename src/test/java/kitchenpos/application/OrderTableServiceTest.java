package kitchenpos.application;

import kitchenpos.domain.order.OrderStatus;
import kitchenpos.domain.ordertable.OrderTable;
import kitchenpos.domain.ordertable.exceptions.InvalidNumberOfGuestsException;
import kitchenpos.domain.ordertable.exceptions.InvalidTryChangeEmptyException;
import kitchenpos.domain.ordertable.exceptions.InvalidTryChangeGuestsException;
import kitchenpos.domain.ordertable.exceptions.OrderTableEntityNotFoundException;
import kitchenpos.ui.dto.order.OrderLineItemRequest;
import kitchenpos.ui.dto.order.OrderRequest;
import kitchenpos.ui.dto.order.OrderResponse;
import kitchenpos.ui.dto.order.OrderStatusChangeRequest;
import kitchenpos.ui.dto.ordertable.*;
import kitchenpos.ui.dto.tablegroup.OrderTableInTableGroupRequest;
import kitchenpos.ui.dto.tablegroup.TableGroupRequest;
import kitchenpos.utils.FixtureUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderTableServiceTest extends FixtureUtils {
    @Autowired
    private OrderTableService orderTableService;

    @Autowired
    private TableGroupService tableGroupService;

    @Autowired
    private OrderService orderService;

    @DisplayName("주문 테이블을 생성할 수 있다.")
    @Test
    void createOrderTableTest() {
        // given
        int numberOfGuests = 0;
        boolean isEmpty = true;

        // when
        OrderTableResponse orderTable = orderTableService.create(new OrderTableRequest(numberOfGuests, isEmpty));

        // then
        assertThat(orderTable.getId()).isNotNull();
        assertThat(orderTable.isEmpty()).isEqualTo(isEmpty);
        assertThat(orderTable.getNumberOfGuests()).isEqualTo(numberOfGuests);
    }

    @DisplayName("주문 테이블 목록을 조회할 수 있다.")
    @Test
    void orderTableListTest() {
        // given
        OrderTableResponse orderTable = this.createOrderTable(true, 0);

        // when
        List<OrderTableResponse> orderTables = orderTableService.list();
        List<Long> ids = orderTables.stream()
                .map(OrderTableResponse::getId)
                .collect(Collectors.toList());

        // then
        assertThat(ids).contains(orderTable.getId());
    }

    @DisplayName("존재하지 않는 주문 테이블의 비움 상태를 바꿀 수 없다.")
    @Test
    void changeEmptyFailWithNotExistOrderTableTest() {
        // given
        Long notExistId = 1000000L;

        // when, then
        assertThatThrownBy(() -> orderTableService.changeEmpty(notExistId, new ChangeEmptyRequest(true)))
                .isInstanceOf(OrderTableEntityNotFoundException.class);
    }

    @DisplayName("단체 지정된 주문 테이블의 비움 상태를 바꿀 수 없다.")
    @Test
    void changeEmptyFailWithGroupedTableTest() {
        // given
        OrderTableResponse orderTable1Response = this.createOrderTable(true, 0);
        OrderTable orderTable1 = orderTableService.findOrderTable(orderTable1Response.getId());
        OrderTableResponse orderTable2Response = this.createOrderTable(true, 0);
        OrderTable orderTable2 = orderTableService.findOrderTable(orderTable2Response.getId());

        TableGroupRequest tableGroupRequest = new TableGroupRequest(Arrays.asList(
                new OrderTableInTableGroupRequest(orderTable1.getId()),
                new OrderTableInTableGroupRequest(orderTable2.getId())
        ));
        tableGroupService.group(tableGroupRequest);

        // when, then
        assertThatThrownBy(() -> orderTableService.changeEmpty(orderTable1.getId(), new ChangeEmptyRequest(true)))
                .isInstanceOf(InvalidTryChangeEmptyException.class)
                .hasMessage("단체 지정된 주문 테이블의 자리 비움 상태를 바꿀 수 없습니다.");
    }

    @DisplayName("주문 상태가 조리거나 식사인 주문 테이블의 비움 상태를 바꿀 수 없다.")
    @Test
    void changeEmptyFailWithInvalidOrderStatusTest() {
        // given
        Long menuId = super.createMenuFixture();

        OrderTableResponse orderTable = this.createOrderTable(false, 3);

        OrderRequest orderRequest = new OrderRequest(
                orderTable.getId(), Collections.singletonList(new OrderLineItemRequest(menuId, 1L)));
        OrderResponse orderResponse = orderService.create(orderRequest);
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.COOKING.name());

        // when, then
        assertThatThrownBy(() -> orderTableService.changeEmpty(orderTable.getId(), new ChangeEmptyRequest(true)))
                .isInstanceOf(InvalidTryChangeEmptyException.class)
                .hasMessage("조리중이거나 식사중인 주문 테이블의 비움 상태를 바꿀 수 없습니다.");
    }

    @DisplayName("주문 테이블의 비움 상태를 바꿀 수 있다.")
    @Test
    void changeEmptyTest() {
        // given
        Long menuId = super.createMenuFixture();

        OrderTableResponse orderTable = this.createOrderTable(false, 3);

        OrderRequest orderRequest = new OrderRequest(
                orderTable.getId(), Collections.singletonList(new OrderLineItemRequest(menuId, 1L)));
        OrderResponse orderResponse = orderService.create(orderRequest);
        orderService.changeOrderStatus(orderResponse.getId(), new OrderStatusChangeRequest(OrderStatus.COMPLETION.name()));

        // when
        OrderTableResponse response = orderTableService.changeEmpty(orderTable.getId(), new ChangeEmptyRequest(true));

        // then
        assertThat(response.isEmpty()).isTrue();
    }

    @DisplayName("방문한 손님 수를 0명 이하로 바꿀 수 없다.")
    @ParameterizedTest
    @ValueSource(ints = { -1, -2 })
    void changeNumberOfGuestsFailWithNegativeValueTest(int invalidValue) {
        // given
        OrderTableResponse orderTable = this.createOrderTable(false, 3);

        // when, then
        assertThatThrownBy(() -> orderTableService.changeNumberOfGuests(orderTable.getId(), new ChangeNumberOfGuestsRequest(invalidValue)))
                .isInstanceOf(InvalidNumberOfGuestsException.class)
                .hasMessage("방문한 손님수는 0명 미만일 수 없습니다.");
    }

    @DisplayName("존재하지 않는 주문 테이블의 방문한 손님 수를 바꿀 수 없다.")
    @Test
    void changeNumberOfGuestsFailWithNotExistOrderTableTest() {
        // given
        Long notExistTableId = 1000000L;

        // when, then
        assertThatThrownBy(() -> orderTableService.changeNumberOfGuests(notExistTableId, new ChangeNumberOfGuestsRequest(500)))
                .isInstanceOf(OrderTableEntityNotFoundException.class)
                .hasMessage("해당 주문 테이블이 존재하지 않습니다.");
    }

    @DisplayName("비어있는 주문 테이블의 방문한 손님 수를 바꿀 수 없다.")
    @Test
    void changeNumberOfGuestsFailWithEmptyOrderTableTest() {
        // given
        OrderTableResponse orderTable = this.createOrderTable(true, 0);

        // when, then
        assertThatThrownBy(() -> orderTableService.changeNumberOfGuests(orderTable.getId(), new ChangeNumberOfGuestsRequest(500)))
                .isInstanceOf(InvalidTryChangeGuestsException.class)
                .hasMessage("비어있는 주문 테이블의 방문한 손님 수를 바꿀 수 없습니다.");
    }

    @DisplayName("주문 테이블의 방문한 손님 수를 바꿀 수 있다.")
    @Test
    void changeNumberOfGuestsTest() {
        // given
        int numberOfGuests = 500;
        OrderTableResponse orderTable = this.createOrderTable(false, 3);

        // when
        OrderTableResponse changed = orderTableService.changeNumberOfGuests(
                orderTable.getId(), new ChangeNumberOfGuestsRequest(numberOfGuests));

        // then
        assertThat(changed.getNumberOfGuests()).isEqualTo(numberOfGuests);
    }

    private OrderTableResponse createOrderTable(final boolean empty, final Integer numberOfGuests) {
        OrderTableRequest orderTableRequest = new OrderTableRequest(numberOfGuests, empty);

        return orderTableService.create(orderTableRequest);
    }
}
