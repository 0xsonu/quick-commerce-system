package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.UpdateCartItemRequest;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant1";
    private static final String USER_ID = "user1";
    private static final String BASE_URL = "/api/v1/cart";

    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        sampleCart = new Cart(TENANT_ID, USER_ID);
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        sampleCart.addItem(item);
        sampleCart.setSubtotal(new BigDecimal("20.00"));
        sampleCart.setTax(new BigDecimal("1.60"));
        sampleCart.setTotal(new BigDecimal("21.60"));
    }

    @Test
    void testGetCart_Success() throws Exception {
        when(cartService.getCart(TENANT_ID, USER_ID)).thenReturn(sampleCart);

        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_id").value(USER_ID))
                .andExpect(jsonPath("$.tenant_id").value(TENANT_ID))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].product_id").value("product1"))
                .andExpect(jsonPath("$.subtotal").value(20.00))
                .andExpect(jsonPath("$.tax").value(1.60))
                .andExpect(jsonPath("$.total").value(21.60));

        verify(cartService).getCart(TENANT_ID, USER_ID);
    }

    @Test
    void testAddToCart_Success() throws Exception {
        AddToCartRequest request = new AddToCartRequest("product2", "sku2", "Product 2", 1, new BigDecimal("15.00"));
        
        when(cartService.addToCart(eq(TENANT_ID), eq(USER_ID), any(AddToCartRequest.class)))
                .thenReturn(sampleCart);

        mockMvc.perform(post(BASE_URL + "/items")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_id").value(USER_ID))
                .andExpect(jsonPath("$.tenant_id").value(TENANT_ID));

        verify(cartService).addToCart(eq(TENANT_ID), eq(USER_ID), any(AddToCartRequest.class));
    }

    @Test
    void testAddToCart_InvalidRequest() throws Exception {
        AddToCartRequest request = new AddToCartRequest("", "sku2", "Product 2", 0, new BigDecimal("-15.00"));

        mockMvc.perform(post(BASE_URL + "/items")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(any(), any(), any());
    }

    @Test
    void testUpdateCartItem_Success() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest("product1", "sku1", 5);
        
        when(cartService.updateCartItem(eq(TENANT_ID), eq(USER_ID), any(UpdateCartItemRequest.class)))
                .thenReturn(sampleCart);

        mockMvc.perform(put(BASE_URL + "/items")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_id").value(USER_ID))
                .andExpect(jsonPath("$.tenant_id").value(TENANT_ID));

        verify(cartService).updateCartItem(eq(TENANT_ID), eq(USER_ID), any(UpdateCartItemRequest.class));
    }

    @Test
    void testUpdateCartItem_InvalidRequest() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest("", "sku1", 0);

        mockMvc.perform(put(BASE_URL + "/items")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).updateCartItem(any(), any(), any());
    }

    @Test
    void testRemoveFromCart_Success() throws Exception {
        when(cartService.removeFromCart(TENANT_ID, USER_ID, "product1", "sku1"))
                .thenReturn(sampleCart);

        mockMvc.perform(delete(BASE_URL + "/items")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID)
                .param("productId", "product1")
                .param("sku", "sku1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_id").value(USER_ID))
                .andExpect(jsonPath("$.tenant_id").value(TENANT_ID));

        verify(cartService).removeFromCart(TENANT_ID, USER_ID, "product1", "sku1");
    }

    @Test
    void testClearCart_Success() throws Exception {
        doNothing().when(cartService).clearCart(TENANT_ID, USER_ID);

        mockMvc.perform(delete(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart(TENANT_ID, USER_ID);
    }

    @Test
    void testDeleteCart_Success() throws Exception {
        doNothing().when(cartService).deleteCart(TENANT_ID, USER_ID);

        mockMvc.perform(delete(BASE_URL + "/delete")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isNoContent());

        verify(cartService).deleteCart(TENANT_ID, USER_ID);
    }

    @Test
    void testCartExists_Success() throws Exception {
        when(cartService.cartExists(TENANT_ID, USER_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/exists")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("true"));

        verify(cartService).cartExists(TENANT_ID, USER_ID);
    }

    @Test
    void testHealth_Success() throws Exception {
        mockMvc.perform(get(BASE_URL + "/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cart Service is healthy"));
    }

    @Test
    void testMissingTenantHeader() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingUserHeader() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidateCart_Success() throws Exception {
        // Given
        doNothing().when(cartService).validateCartForCheckout(TENANT_ID, USER_ID);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/validate")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isOk());

        verify(cartService).validateCartForCheckout(TENANT_ID, USER_ID);
    }

    @Test
    void testValidateCart_ValidationFails() throws Exception {
        // Given
        doThrow(new RuntimeException("Cart validation failed"))
            .when(cartService).validateCartForCheckout(TENANT_ID, USER_ID);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/validate")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isInternalServerError());

        verify(cartService).validateCartForCheckout(TENANT_ID, USER_ID);
    }
}