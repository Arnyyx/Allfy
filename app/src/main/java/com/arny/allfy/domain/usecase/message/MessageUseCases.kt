package com.arny.allfy.domain.usecase.message


data class MessageUseCases(
    val loadConversations: LoadConversations,
    val markMessageAsRead: MarkMessageAsRead,
    val sendMessage: SendMessage,
    val sendImages: SendImages,
    val getMessages: GetMessages,
    val sendVoiceMessage: SendVoiceMessage,
    val initializeConversation: InitializeConversation
)