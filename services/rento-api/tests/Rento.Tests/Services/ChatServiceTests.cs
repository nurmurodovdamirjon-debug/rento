using Moq;
using Rento.Application.Repositories;
using Rento.Application.Services.Implementations;
using Rento.Core.Common;
using Rento.Core.Entities;
using Xunit;

namespace Rento.Tests.Services;

public sealed class ChatServiceTests
{
    private readonly Mock<IChatRepository> _chatRepo = new();
    private readonly Mock<IListingRepository> _listingRepo = new();

    [Fact]
    public async Task CreateOrGetRoomAsync_WhenListingMissing_ReturnsNull()
    {
        var listingId = Guid.NewGuid();
        _listingRepo.Setup(r => r.GetByIdWithImagesAsync(listingId, It.IsAny<CancellationToken>()))
            .ReturnsAsync((Listing?)null);

        var sut = new ChatService(_chatRepo.Object, _listingRepo.Object);
        var result = await sut.CreateOrGetRoomAsync(listingId, Guid.NewGuid(), null);

        Assert.Null(result);
    }

    [Fact]
    public async Task CreateOrGetRoomAsync_WhenUserOwnsListing_ReturnsNull()
    {
        var listingId = Guid.NewGuid();
        var userId = Guid.NewGuid();

        _listingRepo.Setup(r => r.GetByIdWithImagesAsync(listingId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Listing
            {
                Id = listingId,
                UserId = userId,
                Type = "apartment",
                City = "Tashkent",
                Title = "Own listing"
            });

        var sut = new ChatService(_chatRepo.Object, _listingRepo.Object);
        var result = await sut.CreateOrGetRoomAsync(listingId, userId, "hello");

        Assert.Null(result);
    }

    [Fact]
    public async Task SendMessageAsync_WhenTypeMissing_UsesTextDefault()
    {
        var roomId = Guid.NewGuid();
        var userId = Guid.NewGuid();

        _chatRepo.Setup(r => r.IsUserInRoomAsync(roomId, userId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(true);

        _chatRepo.Setup(r => r.AddMessageAsync(It.IsAny<Message>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync((Message msg, CancellationToken _) =>
            {
                msg.Id = Guid.NewGuid();
                msg.CreatedAt = DateTime.UtcNow;
                return msg;
            });

        _chatRepo.Setup(r => r.UpdateRoomLastMessageAtAsync(roomId, It.IsAny<DateTime>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var sut = new ChatService(_chatRepo.Object, _listingRepo.Object);
        var result = await sut.SendMessageAsync(roomId, userId, "hello", null, null, null);

        Assert.NotNull(result);
        Assert.Equal(MessageType.Text, result.MessageType);
        _chatRepo.Verify(
            r => r.AddMessageAsync(It.Is<Message>(m => m.MessageType == MessageType.Text), It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task GetMessagesAsync_WhenUserNotInRoom_ReturnsNull()
    {
        var roomId = Guid.NewGuid();
        var userId = Guid.NewGuid();

        _chatRepo.Setup(r => r.IsUserInRoomAsync(roomId, userId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(false);

        var sut = new ChatService(_chatRepo.Object, _listingRepo.Object);
        var result = await sut.GetMessagesAsync(roomId, userId, 1, 20);

        Assert.Null(result);
    }
}
