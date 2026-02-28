using System.Security.Claims;
using Microsoft.AspNetCore.SignalR;
using Moq;
using Rento.Api.Hubs;
using Rento.Application.Services;
using Xunit;

namespace Rento.Tests.Hubs;

public sealed class ChatHubTests
{
    private readonly Mock<IChatService> _chatService = new();
    private readonly Mock<IHubCallerClients> _clients = new();
    private readonly Mock<ISingleClientProxy> _caller = new();
    private readonly Mock<IGroupManager> _groups = new();
    private readonly Mock<HubCallerContext> _context = new();

    [Fact]
    public async Task JoinRoom_WhenUserMissing_SendsUnauthorizedError()
    {
        var hub = CreateHub(user: null, connectionId: "conn-1");

        await hub.JoinRoom(Guid.NewGuid().ToString());

        _caller.Verify(c => c.SendCoreAsync("Error", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()), Times.Once);
        _groups.Verify(g => g.AddToGroupAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task JoinRoom_WhenRoomIdInvalid_SendsError()
    {
        var hub = CreateHub(CreateUser(Guid.NewGuid()), "conn-2");

        await hub.JoinRoom("not-a-guid");

        _caller.Verify(c => c.SendCoreAsync("Error", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()), Times.Once);
        _groups.Verify(g => g.AddToGroupAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task JoinRoom_WhenValidAndAuthorized_AddsConnectionToGroup()
    {
        var roomId = Guid.NewGuid();
        var userId = Guid.NewGuid();

        _chatService.Setup(s => s.IsUserInRoomAsync(roomId, userId, It.IsAny<CancellationToken>()))
            .ReturnsAsync(true);

        var hub = CreateHub(CreateUser(userId), "conn-3");

        await hub.JoinRoom(roomId.ToString());

        _groups.Verify(g => g.AddToGroupAsync("conn-3", roomId.ToString(), It.IsAny<CancellationToken>()), Times.Once);
    }

    private ChatHub CreateHub(ClaimsPrincipal? user, string connectionId)
    {
        _clients.SetupGet(c => c.Caller).Returns(_caller.Object);
        _context.SetupGet(c => c.User).Returns(user);
        _context.SetupGet(c => c.ConnectionId).Returns(connectionId);
        _context.SetupGet(c => c.ConnectionAborted).Returns(CancellationToken.None);

        return new ChatHub(_chatService.Object)
        {
            Clients = _clients.Object,
            Context = _context.Object,
            Groups = _groups.Object
        };
    }

    private static ClaimsPrincipal CreateUser(Guid userId)
    {
        return new ClaimsPrincipal(new ClaimsIdentity(
            [new Claim(ClaimTypes.NameIdentifier, userId.ToString())],
            "test"));
    }
}
